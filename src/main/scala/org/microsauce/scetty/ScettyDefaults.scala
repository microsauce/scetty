package org.microsauce.scetty

import java.net.InetAddress
import javax.net.ssl.SSLContext

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel

object ScettyDefaults {
  val inetAddressName: String = "localhost";
  val port: Int = 80
  val maxInitialLineLength: Int = 4096
  val maxHeaderSize: Int = 8192
  val maxChunkSize: Int = 8192
  val maxContentLength: Int = 65536
//  val channelInitializer: ChannelInitializer[SocketChannel] = new HttpServerInitializer(routeHandler)

  val ssl: Boolean = false
  val sslKeystore: String = ""
  val keypass: String = ""
//  private var sslProvider: () => SSLContext = null
//  private val routeHandler: HttpRouteMiddlewareRequestHandler = new HttpRouteMiddlewareRequestHandler

}
