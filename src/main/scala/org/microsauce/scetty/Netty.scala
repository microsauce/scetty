package org.microsauce.scetty

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelPipeline
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.stream.ChunkedWriteHandler
import io.netty.handler.ssl.SslHandler
import javax.net.ssl.{SSLEngine, SSLContext}
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.handler.codec.http.HttpRequestDecoder
import java.net.InetAddress

/**
 * Netty is a convenience class that encapsulates the netty bootstrap and channel 
 * initializer.  It provides a default HTTP/HTTPS channel initializer and an
 * intuitive/user-friendly API for instantiating and configuring Netty HTTP/HTTPS servers.
 */
class Netty {

  private var _inetAddress:InetAddress = InetAddress.getLocalHost
  private var _port = 80
  private var _maxInitialLineLength = 4096
  private var _maxHeaderSize = 8192
  private var _maxChunkSize = 8192
  private var _maxContentLength = 65536
  private var _channelInitializer:ChannelInitializer[SocketChannel] = new HttpServerInitializer(routeHandler)

  private var _ssl = false
  private var _sslKeystore = ""
  private var _keypass = ""
  private var sslProvider:()=>SSLContext = null
  private val routeHandler: HttpRouteMiddlewareRequestHandler = new HttpRouteMiddlewareRequestHandler

  //
  // server config
  //

  /**
   * Set the address on which to bind the server - default is java.net.InetAddress.anyLocalAddress
   */
  def address(host:String) = {
    _inetAddress = InetAddress.getByName(host)
    this
  }

  /**
   * Set the port on which to bind the server; default is 80
   */
  def port(port:Int) = {
    _port = port
    this
  }

  /**
   * Set the HttpRequestDecoder maxInitialLineLength; default is 4096
   */
  def maxInitialLineLength(maxInitialLineLength:Int) = {
    _maxInitialLineLength = maxInitialLineLength
    this
  }

  /**
   * Set the HttpRequestDecoder maxHeaderSize; default is 8192
   * @param maxHeaderSize
   * @return
   */
  def maxHeaderSize(maxHeaderSize:Int) = {
    _maxHeaderSize = maxHeaderSize
    this
  }

  /**
   * Set the HttpRequestDecoder maxChunkSize; default is 8192
   * @param maxChunkSize
   * @return
   */
  def maxChunkSize(maxChunkSize:Int) = {
    _maxChunkSize = maxChunkSize
    this
  }

  /**
   * Set the HttpObjectAggregator maxContentLength value:  default is 65536
   * @param maxContentLength
   */
  def maxContentLength(maxContentLength:Int) = {
    _maxContentLength = maxContentLength
    this
  }

  //
  // https config
  //

  /**
   * Set the SSL flag - default is false
   */
  def ssl(sslEnabled:Boolean) = {
    _ssl = sslEnabled
    this
  }

  /**
   * Set the path to java keystore - used to initialize the javax SSLContext
   */
  def keystore(keystore:String) = {
    _sslKeystore = keystore
    this
  }

  /**
   * Set the java keystore password - used to initialize the javax SSLContext
   */
  def keypass(keypass:String) = {
    _keypass = keypass
    this
  } 
  
  //
  // add routers
  //

  /**
   * Add a router to this Netty instance, multiple routers are supported.
   * @param router
   * @return
   */
  def router(router:Router) = {
    routeHandler.add(router)
    this
  }

  /**
   * Set the channel initializer for this Netty server instance.  For HTTP servers this defaults to:
   * - p.addLast("decoder", new HttpRequestDecoder(_maxInitialLineLength,_maxHeaderSize,_maxChunkSize,true))
   * - p.addLast("aggregator", new HttpObjectAggregator(_maxContentLength))
   * - p.addLast("encoder", new HttpResponseEncoder())
   * - p.addLast("chunkedWriter", new ChunkedWriteHandler())
   * - p.addLast("handler", routeHandler)  // routeHandler => HttpRouteMiddlewareRequestHandler (@Shared)
   *
   * For ssl servers this defaults to:
   * - p.addLast("decoder", new HttpRequestDecoder(_maxInitialLineLength,_maxHeaderSize,_maxChunkSize,true))
   * - p.addLast("aggregator", new HttpObjectAggregator(_maxContentLength))
   * - p.addLast("encoder", new HttpResponseEncoder())
   * - p.addLast("handler", routeHandler)  // routeHandler => HttpRouteMiddlewareRequestHandler (@Shared)
   *
   * @param initializer
   * @return
   */
  def channelInit(initializer:ChannelInitializer[SocketChannel]) = {
    _channelInitializer = initializer
    this
  }

  private def serverBootstrap () = {
    val bossGroup = new NioEventLoopGroup()
    val workerGroup = new NioEventLoopGroup()
    try {
      val bootstrap = new ServerBootstrap()
      bootstrap.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .childHandler(_channelInitializer)
      val channel = bootstrap.bind(_inetAddress, _port).sync().channel()
      channel.closeFuture().sync()
    } finally {
      bossGroup.shutdownGracefully()
      workerGroup.shutdownGracefully()
    }
  }

  /**
   * The default channel pipeline initializer.
   * @param handler
   */
  class HttpServerInitializer(handler: HttpRouteMiddlewareRequestHandler) extends ChannelInitializer[SocketChannel] {

    private var sslEngine:SSLEngine = null

    private def getSSLEngine = {
      if ( sslEngine == null ) {
        sslEngine = sslProvider().createSSLEngine
        sslEngine.setUseClientMode(false)
        sslEngine
      } else sslEngine
    }

    val chunkedResponsePipeline: ChannelPipeline => Unit = { p =>
      p.addLast("decoder", new HttpRequestDecoder(_maxInitialLineLength,_maxHeaderSize,_maxChunkSize,true))
      p.addLast("aggregator", new HttpObjectAggregator(_maxContentLength))
      p.addLast("encoder", new HttpResponseEncoder())
      p.addLast("chunkedWriter", new ChunkedWriteHandler())
      p.addLast("handler", routeHandler)
    }
    
    val nonChunkedResponsePipeline: ChannelPipeline => Unit = { p =>
      p.addLast("decoder", new HttpRequestDecoder(_maxInitialLineLength,_maxHeaderSize,_maxChunkSize,true))
      p.addLast("aggregator", new HttpObjectAggregator(_maxContentLength))
      p.addLast("encoder", new HttpResponseEncoder())
      p.addLast("handler", routeHandler)
    }

    var responsePipeline: ChannelPipeline => Unit = null

    override def initChannel(ch: SocketChannel) {
      val p = ch.pipeline()

      val pipeline = if ( _ssl ) {
	      p.addLast("ssl", new SslHandler(getSSLEngine))
	      nonChunkedResponsePipeline
      } else { chunkedResponsePipeline }
        
      pipeline(p)
    }
  }

  /**
   * Start this Netty server instance.
   */
  def start { 
    if ( _ssl ) {
      sslProvider = SSLProviderFactory.getProvider(_sslKeystore, _keypass)
    }
    serverBootstrap()
  }

}





