package org.microsauce.scetty

import java.net.InetAddress
import javax.net.ssl.{SSLContext, SSLEngine}

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{Channel, ChannelInitializer, ChannelPipeline}
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpRequestDecoder, HttpResponseEncoder}
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedWriteHandler

/**
  * Scetty is a netty HTTP server builder that encapsulates the netty bootstrap and channel
  * initializer.  It provides a default HTTP/HTTPS channel initializer and an
  * intuitive/user-friendly API for instantiating and configuring Netty HTTP/HTTPS servers.
  */
class Scetty(var ctx: ScettyContext) {

  private var sslProvider: () => SSLContext = _
  private var routeHandler: HttpRouteMiddlewareRequestHandler = _
  private var channelInitializer: ChannelInitializer[SocketChannel] = new HttpServerInitializer(routeHandler)
  private var contextBuilder: ScettyContextBuilder = new ScettyContextBuilder()
  private var channel: Channel = _

  def this() {
    this(null)
  }

  private def throwIllegalStateException: Unit = {
    throw new IllegalStateException("Scetty context already assigned")
  }

  def prependRouter(router: Router): Scetty = {
    if (ctx != null) throwIllegalStateException

    contextBuilder.prependRouter(router)
    this
  }

  def addRouter(router: Router): Scetty = {
    if (ctx != null) throwIllegalStateException

    contextBuilder.addRouter(router)
    this
  }

  /**
    * Set the address on which to bind the server
    */
  def inetAddress(inetAddress: String): Scetty = {
    if (ctx != null) throwIllegalStateException

    contextBuilder.inetAddress(inetAddress)
    this
  }

  /**
    * Set the port on which to bind the server; default is 80
    */
  def port(port: Int): Scetty = {
    if (ctx != null) throwIllegalStateException

    contextBuilder.port(port)
    this
  }

  /**
    * Set the HttpRequestDecoder maxInitialLineLength; default is 4096
    */
  def maxInitialLineLength(maxInitialLineLength: Int): Scetty = {
    if (ctx != null) throwIllegalStateException

    contextBuilder.maxInitialLineLength(maxInitialLineLength)
    this
  }

  /**
    * Set the HttpRequestDecoder maxHeaderSize; default is 8192
    *
    * @param maxHeaderSize
    * @return
    */
  def maxHeaderSize(maxHeaderSize: Int): Scetty = {
    if (ctx != null) throwIllegalStateException

    contextBuilder.maxHeaderSize(maxHeaderSize)
    this
  }

  /**
    * Set the HttpRequestDecoder maxChunkSize; default is 8192
    *
    * @param maxChunkSize
    * @return
    */
  def maxChunkSize(maxChunkSize: Int): Scetty = {
    if (ctx != null) throwIllegalStateException

    contextBuilder.maxChunkSize(maxChunkSize)
    this
  }

  /**
    * Set the HttpObjectAggregator maxContentLength value:  default is 65536
    *
    * @param maxContentLength
    */
  def maxContentLength(maxContentLength: Int): Scetty = {
    if (ctx != null) throwIllegalStateException

    contextBuilder.maxContentLength(maxContentLength)
    this
  }

  def dataFactoryMinSize(dataFactoryMinSize: Long): Scetty = {
    if (ctx != null) throwIllegalStateException

    contextBuilder.dataFactoryMinSize(dataFactoryMinSize)
    this
  }

  /**
    * Set the SSL flag - default is false
    */
  def ssl(sslEnabled: Boolean): Scetty = {
    if (ctx != null) throwIllegalStateException

    contextBuilder.ssl(sslEnabled)
    this
  }

  /**
    * Set the path to java keystore - used to initialize the javax SSLContext
    */
  def keystore(keystore: String): Scetty = {
    if (ctx != null) throwIllegalStateException

    contextBuilder.keystore(keystore)
    this
  }

  /**
    * Set the java keystore password - used to initialize the javax SSLContext
    */
  def keypass(keypass: String): Scetty = {
    if (ctx != null) throwIllegalStateException

    contextBuilder.keypass(keypass)
    this
  }

  /**
    * Set the channel initializer for this Netty server instance.
    *
    * @param initializer
    * @return
    */
  def channelInit(initializer: ChannelInitializer[SocketChannel]): Scetty = {
    if (ctx != null) throwIllegalStateException

    channelInitializer = initializer
    this
  }

  /**
    * Start this Netty server instance.
    */
  def start: Scetty = {

    ctx = if (ctx == null)
      contextBuilder.build
    else
      ctx

    routeHandler = new HttpRouteMiddlewareRequestHandler(ctx.dataFactoryMinSize)
    routeHandler.uriRouters ++= ctx.routers

    if (ctx.ssl) {
      sslProvider = SSLProviderFactory.getProvider(ctx.sslKeystore, ctx.keypass)
    }

    serverBootstrap()

    this
  }

  def stop: Unit = {
    this.channel.close
  }

  private def serverBootstrap() = {

    val acceptorGroup = new NioEventLoopGroup()
    val clientGroup = new NioEventLoopGroup()

    val serverBootstrap = new ServerBootstrap()
    serverBootstrap.group(acceptorGroup, clientGroup)
      .channel(classOf[NioServerSocketChannel])
      .childHandler(channelInitializer)

    try {
      val netInetAddress = InetAddress.getByName(ctx.inetAddress)
      val channelFuture = serverBootstrap.bind(netInetAddress, ctx.port)

      this.channel = channelFuture.sync().channel()

      this.channel.closeFuture().sync()
    } finally {
      acceptorGroup.shutdownGracefully()
      clientGroup.shutdownGracefully()
    }
  }

  /**
    * The default channel pipeline initializer
    *
    * @param handler
    */
  class HttpServerInitializer(handler: HttpRouteMiddlewareRequestHandler)
    extends ChannelInitializer[SocketChannel] {

    private var sslEngine: SSLEngine = null

    private def getSSLEngine = {
      if (sslEngine == null) {
        sslEngine = sslProvider().createSSLEngine
        sslEngine.setUseClientMode(false)
        sslEngine
      } else sslEngine
    }

    val chunkedResponsePipeline: ChannelPipeline => Unit = { p =>
      p.addLast("decoder", new HttpRequestDecoder(
        ctx.maxInitialLineLength, ctx.maxHeaderSize, ctx.maxChunkSize, true))
      p.addLast("aggregator", new HttpObjectAggregator(ctx.maxContentLength))
      p.addLast("encoder", new HttpResponseEncoder())
      p.addLast("chunkedWriter", new ChunkedWriteHandler())
      p.addLast("handler", routeHandler)
    }

    val nonChunkedResponsePipeline: ChannelPipeline => Unit = { p =>
      p.addLast("decoder", new HttpRequestDecoder(
        ctx.maxInitialLineLength, ctx.maxHeaderSize, ctx.maxChunkSize, true))
      p.addLast("aggregator", new HttpObjectAggregator(ctx.maxContentLength))
      p.addLast("encoder", new HttpResponseEncoder())
      p.addLast("handler", routeHandler)
    }

    var responsePipeline: ChannelPipeline => Unit = null

    override def initChannel(ch: SocketChannel) {
      val p = ch.pipeline()

      val pipeline = if (ctx.ssl) {
        p.addLast("ssl", new SslHandler(getSSLEngine))
        nonChunkedResponsePipeline
      } else {
        chunkedResponsePipeline
      }

      pipeline(p)
    }
  }

}





