package org.microsauce.scetty

//import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.buffer.ByteBuf
import io.netty.channel.DefaultFileRegion
import java.io.RandomAccessFile
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.Cookie
import java.io.File
import scala.concurrent.Future
import org.apache.commons.codec.Charsets
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.websocketx.WebSocketFrame

/**
 * This class models an HTTP response.
 *
 * Response
 * - HttpResponse
 * - TextFrameResponse
 * - BinaryFrameResponse
 */
class Response(val status:HttpResponseStatus, val source:Any, val contentType:String) {

  import io.netty.handler.codec.http.HttpHeaders.Names._
  import io.netty.handler.codec.http.HttpHeaders._
  import io.netty.handler.codec.http.HttpResponseStatus._
  import io.netty.handler.codec.http.HttpVersion._
  import scala.concurrent.Promise

  private val _cookies = scala.collection.mutable.Map[String,Cookie]()

  def getCookies = _cookies

  /**
   * @return an already completed Future[Response]
   */
  def toFuture = Future.successful(this)

  /**
   * The response body source
   */
  val nettySource = source match {
    case str:String => Unpooled.copiedBuffer(str, Charsets.UTF_8)
    case file:File =>
      try {
        val raf = new RandomAccessFile(file, "r")
        new DefaultFileRegion(raf.getChannel(), 0, raf.length())
      } catch {
        case t: Throwable => Unpooled.copiedBuffer(s"error: ${t.getMessage}", Charsets.UTF_8)
      }
    case buf:ByteBuf => Unpooled.copiedBuffer(buf)
    case bytes:Array[Byte] => Unpooled.copiedBuffer(bytes)
    case _ => Unpooled.copiedBuffer("", Charsets.UTF_8) // null content or Redirect
  }

  /**
   * The underlying DefaultHttpResponse instance
   */
  val nettyResponse = new DefaultHttpResponse(HTTP_1_1,status)
  nettyResponse.headers().set(CONTENT_TYPE, contentType)


  def cookies(cookies:Map[String,Cookie]) = {
    _cookies ++= cookies
  }

  def cookie(cookie:Cookie) = {
    _cookies += cookie.getName -> cookie
    this
  }

}