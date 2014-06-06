package org.microsauce.scetty

import org.apache.commons.codec.Charsets
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.handler.stream.ChunkedWriteHandler
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.channel.DefaultFileRegion
import java.io.RandomAccessFile
import scala.io.Source
import java.net.URL
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE
import io.netty.handler.codec.http.HttpResponseStatus.OK
import io.netty.handler.codec.http.HttpVersion.HTTP_1_1

object TestNetty extends App {

  class MimeTable {
    private val map = scala.collection.mutable.HashMap[String, String]()
    def add(line: String) {
      val tokens = line.split(" ")
      val mimeString = tokens.head
      val fileExtensions = tokens.tail
      fileExtensions.foreach(map(_) = mimeString)
    }
    def getType(ext: String) = {
      val typ = map(ext)
      if (typ == null) "application/octet-stream"
      else typ
    }
  }

  //
  // initialize the mime table
  //
  println("initializing mime table . . .")
  val mimeTypesMap = new MimeTable()
  val mtis = TestNetty.getClass.getClassLoader.getResourceAsStream("mime.table")
  Source.fromInputStream(mtis).getLines.foreach(mimeTypesMap.add(_))
  println("\tdone")

  val routeHandler: TestHandler = new TestHandler()

  class HttpServerInitializer(handler: TestHandler) extends ChannelInitializer[SocketChannel] {

    val defaultPipeline: ChannelPipeline => Unit = { p =>
      p.addLast("decoder", new HttpRequestDecoder())
      p.addLast("aggregator", new HttpObjectAggregator(65536))
      p.addLast("encoder", new HttpResponseEncoder())
      p.addLast("handler", routeHandler)
      p.addLast("chunkedWriter", new ChunkedWriteHandler())
    }

    override def initChannel(ch: SocketChannel) {
      val p = ch.pipeline();

      defaultPipeline(p)

    }
  }

  @Sharable
  class TestHandler extends SimpleChannelInboundHandler[FullHttpRequest] {

    import UriUtils._
    import scala.concurrent._
    import scala.io._
    import scala.concurrent.ExecutionContext.Implicits.global
    import scala.concurrent.duration._
    import scala.util.{ Try, Success, Failure }
    import io.netty.handler.codec.http.HttpHeaders.Names._
    import io.netty.handler.codec.http.HttpHeaders._
    import io.netty.handler.codec.http.HttpResponseStatus._
    import io.netty.handler.codec.http.HttpVersion._

    var counter = 0
    private implicit def makeChannelFutureListener(listener: (ChannelFuture) => Unit) =
      new ChannelFutureListener {
        override def operationComplete(channel: ChannelFuture) { listener(channel) }
      }

    override def channelReadComplete(ctx: ChannelHandlerContext) {
      
      ctx.flush()
    }

    private val closeChannel:ChannelFuture=>Unit = { future =>
      println (s"closing channel ${future.channel}")
      println (s"is open (before): ${future.channel.isOpen}")
      future.channel.close()
      println (s"is open  (after): ${future.channel.isOpen}")
    }

    override def channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {

      var content: Any = null

      request.getUri match {
        case "/" =>
          println("handling /")
          counter += 1
          val response = new DefaultFullHttpResponse(HTTP_1_1, OK) //Unpooled.copiedBuffer(contentBuffer))
          response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8") // TODO
          val content = response.content()
          content.writeBytes(s";) hello /! => $counter (;".getBytes)

          ctx.writeAndFlush(response).addListener(closeChannel)
          
        //            ctx.writeAndFlush(Unpooled.copiedBuffer(s";) hello /! => $counter", Charsets.UTF_8))
        case "/file" =>
          // TODO for multipart writes use DefaultHttpResponse 
          counter += 1
          val response = new DefaultHttpResponse(HTTP_1_1, OK) //Unpooled.copiedBuffer(contentBuffer))
          //            response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8") // TODO
          val raf = try {
            new RandomAccessFile("/home/jboone/Dropbox/buba_board.jpg", "r")
          } catch {
            case t: Throwable => null
          }
          println("mimetype: " + mimeTypesMap.getType("jpg"))
          response.headers.set(CONTENT_TYPE, mimeTypesMap.getType("jpg"))

          val cf1 = ctx.write(response)
          println(s"cf1: $cf1")
          val cf2 = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, raf.length()) /*, ctx.newProgressivePromise()*/ )
  
          println(s"cf2: $cf2")

          println(s"c1: ${cf1.channel}")
          println(s"c2: ${cf2.channel}")
          cf2.addListener(closeChannel) 
        case "/string" =>
          counter += 1
          val response = new DefaultHttpResponse(HTTP_1_1, OK) //Unpooled.copiedBuffer(contentBuffer))
          response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8") // TODO

          ctx.writeAndFlush(response)
          ctx.write(Unpooled.copiedBuffer(s"hello string woot => $counter", Charsets.UTF_8)).addListener(closeChannel)
        case "/url" =>
          val source = Source.fromURL(new URL("http://www.google.com")).getLines.mkString("\n")
          counter += 1
          val response = new DefaultHttpResponse(HTTP_1_1, OK) //Unpooled.copiedBuffer(contentBuffer))
          response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8") // TODO

          ctx.write(response)
          ctx.write(Unpooled.copiedBuffer(source, Charsets.UTF_8)).addListener(closeChannel)
        case _ =>
          counter += 1
          val response = new DefaultFullHttpResponse(HTTP_1_1, OK, // TODO
            Unpooled.copiedBuffer(s"hello default => $counter", Charsets.UTF_8)) //Unpooled.copiedBuffer(contentBuffer))
          response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8") // TODO

          ctx.writeAndFlush(response).addListener(closeChannel)

      }
    }

  }

  val bossGroup = new NioEventLoopGroup()
  val workerGroup = new NioEventLoopGroup()
  try {
    val bootstrap = new ServerBootstrap()
    bootstrap.group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .childHandler(new HttpServerInitializer(routeHandler))
    val channel = bootstrap.bind(8081).sync().channel()
    channel.closeFuture().sync()
  } finally {
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
  }

}