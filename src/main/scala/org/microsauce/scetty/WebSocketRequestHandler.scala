package org.microsauce.scetty

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.{HttpHeaders, FullHttpRequest}
import io.netty.handler.codec.http.websocketx._
import scala.collection.mutable.ListBuffer


//import io.netty.channel.ChannelHandler.Sharable

/**
 * TODO

 *  - howto register channels
 *  - handshaker lifecycle
 *  - add channel property to Request class
 *
 *  - one handler per channel ??? i think so
 *
 * Created by jboone on 2/12/14.
 */

// TODO params: uri, messageCoder, callback
class WebSocketRequestHandler(val webSocketHandler:WebSocketHandler) extends SimpleChannelInboundHandler[AnyRef] {

  private var handShaker:WebSocketServerHandshaker = null
  private val stringBuffer = new StringBuilder()
  private var byteBuffer:ListBuffer[Byte] = new ListBuffer[Byte]
  private var nettyRequest:FullHttpRequest = null
  private var serviceUri:String = null

  override def channelRead0(ctx: ChannelHandlerContext, message: AnyRef):Unit = message match {
    case httpRequest:FullHttpRequest => handShake(ctx,httpRequest)
    case socketFrame:WebSocketFrame  => handleSocketFrame(ctx,socketFrame)
  }

  override def channelReadComplete(ctx: ChannelHandlerContext):Unit = ctx.flush

  private def handShake(ctx:ChannelHandlerContext,httpRequest:FullHttpRequest) {
    import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory._

    nettyRequest = httpRequest
    val wsFactory = new WebSocketServerHandshakerFactory(
      s"ws://${httpRequest.headers.get(HttpHeaders.Names.HOST)}${httpRequest.getUri}", null, false)
    handShaker = wsFactory.newHandshaker(httpRequest)
    if (handShaker == null) sendUnsupportedWebSocketVersionResponse(ctx.channel())
    else handShaker.handshake(ctx.channel(), httpRequest)
  }

  private def handleSocketFrame1(ctx:ChannelHandlerContext,socketFrame:WebSocketFrame) {
    byteBuffer ++= socketFrame.content.array
    if ( socketFrame.isFinalFragment ) {
      // TODO handleMessage (WebSocketRouter handler)
ctx.channel().write(new TextWebSocketFrame(new String(byteBuffer.toArray).toUpperCase))
      byteBuffer.clear
    }
  }

  private def handleSocketFrame(ctx:ChannelHandlerContext,socketFrame:WebSocketFrame):Unit = socketFrame match {
    case textFrame:TextWebSocketFrame =>
      stringBuffer append textFrame.text
      if ( textFrame.isFinalFragment ) {
        handleTextMessage(stringBuffer.toString)
        stringBuffer.clear
      }
    case binaryFrame:BinaryWebSocketFrame =>
      byteBuffer ++= binaryFrame.content.array
      if ( binaryFrame.isFinalFragment ) {
        handleBinaryMessage(byteBuffer.toArray)
        byteBuffer.clear
      }
    case closeFrame:CloseWebSocketFrame =>
      handShaker.close(ctx.channel(), closeFrame.retain)
    case continuationFrame:ContinuationWebSocketFrame =>
//      byteBuffer ++= continuationFrame.content.array
      if ( continuationFrame.isFinalFragment ) {
        if ( continuationFrame.aggregatedText != null ) {
          handleTextMessage(continuationFrame.aggregatedText)
        } else {
          val messageContent = new Array[Byte](continuationFrame.content.readableBytes)
          continuationFrame.content.readBytes(messageContent)
          handleBinaryMessage(messageContent)
        }
//        // TODO handleMessage (WebSocketRouter handler)
//        byteBuffer.clear
      }
  }

  private def handleTextMessage(textMessage:String) {
    // TODO
  }

  private def handleBinaryMessage(binaryMessage:Array[Byte]) {
    // TODO
  }
}
