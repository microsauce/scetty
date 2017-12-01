package org.microsauce.scetty

import java.io.File

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.handler.codec.http.{Cookie, DefaultHttpResponse, HttpResponseStatus}
import io.netty.handler.stream.ChunkedFile
import org.apache.commons.codec.Charsets

import scala.concurrent.Future

/**
 * This class models an HTTP response.
 *
 * Response
 * - HttpResponse
 * - TextFrameResponse
 * - BinaryFrameResponse
 */
class Response(val status:HttpResponseStatus, var source:Any, val contentType:String) {

  import io.netty.handler.codec.http.HttpHeaders.Names._
  import io.netty.handler.codec.http.HttpVersion._

  private val _cookies = scala.collection.mutable.Map[String,Cookie]()

  def getCookies = _cookies

  /**
   * @return an already completed Future[Response]
   */
  def toFuture = Future.successful(this)

  def toNettySource = source match {
    case str:String => Unpooled.copiedBuffer(str, Charsets.UTF_8)
    case file:File => new ChunkedFile(file)
    //      try {
    //        val raf = new RandomAccessFile(file, "r")
    //        new DefaultFileRegion(raf.getChannel(), 0, raf.length())
    //      } catch {
    //        case t: Throwable => Unpooled.copiedBuffer(s"error: ${t.getMessage}", Charsets.UTF_8)
    //      }
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