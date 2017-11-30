package org.microsauce.scetty

import io.netty.handler.codec.http.multipart.{Attribute, DefaultHttpDataFactory, FileUpload, HttpPostRequestDecoder}
import io.netty.handler.codec.http._

import scala.collection.mutable.ListBuffer
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder
import UriUtils._
import io.netty.handler.codec.http.HttpHeaders.Names._
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}

import scala.concurrent.Future

/**
  * This class decorates the Netty FullHttpRequest providing additional functionality:
  *
  * - uri parameter map
  * - mutable request attribute map
  * - json serializer/deserializer
  * - query string map
  * - form map
  * - cookie map
  * - next method - execute the next handler in the route
  */
class Request(
     val verb: HttpVerb,
     val req: FullHttpRequest,
     val route: ListBuffer[HttpRequestHandler],
     val dataFactory: DefaultHttpDataFactory,
     val error: Throwable) {

  implicit val formats = Serialization.formats(NoTypeHints)

  private val queryStringDecoder = new QueryStringDecoder(req.getUri)
  private var uriParameters: Map[String, String] = null
  private var routeCursor = -1
  private val attr = scala.collection.mutable.Map[String, Any]()
  private val postRequestDecoder: HttpPostRequestDecoder = if (verb == POST || verb == PUT)
    new HttpPostRequestDecoder(dataFactory, req)
  else null

  /**
    * The mutable cookie map.
    */
  val cookies = scala.collection.mutable.Map[String, Cookie]()

  /**
    * Retrieve a form parameter value with the given name.
    *
    * @param key
    * @return
    */
  def &(key: String): Option[String] = {
    if (postRequestDecoder != null) {
      postRequestDecoder.getBodyHttpData(key) match {
        case x: Attribute => Some(x.getValue)
        case _ => None
      }
    } else None
  }

  def ^^(key: String): Option[ByteBuf] = {
    if (postRequestDecoder != null) {
      postRequestDecoder.getBodyHttpData(key) match {
        case x: FileUpload => Some(x.getByteBuf)
        case _ => None
      }
    } else None
  }

  /**
    * Retrieve a query string parameter value by key.
    *
    * @param key
    * @return
    */
  def ?(key: String): Option[String] = {
    val value = queryStringDecoder.parameters.get(key)

    if (value != null && value.size > 0) Some(value.get(0))
    else None
  }

  /**
    * Deserialize the (json) request body as an object of the given type.
    *
    * @tparam T
    * @return
    */
  def json[T: Manifest]: T = {
    assert(contentType.startsWith("application/json"), s"Content-Type application/json expected, instead found $contentType")
    read[T](bodyString)
  }

  /**
    * The request body content as a String
    *
    * @return body content
    */
  def bodyString: String = {
    content.toString(java.nio.charset.Charset.forName("utf-8"))
  }

  /**
    * Retrieve the Content-Type header value
    *
    * @return
    */
  def contentType: String = req.headers.get(CONTENT_TYPE)

  /**
    * @see <a href="http://netty.io/5.0/api/io/netty/buffer/ByteBufHolder.html#content()">netty.io</a>
    * @return
    */
  def content: ByteBuf = req.content

  /**
    * @see <a href="http://netty.io/5.0/api/io/netty/handler/codec/http/FullHttpRequest.html#copy()">netty.io</a>
    * @return
    */
  def copy: Request = new Request(verb, req.copy, route, dataFactory, this.error)

  /**
    * @see <a href="http://netty.io/5.0/api/io/netty/handler/codec/http/FullHttpRequest.html#duplicate()">netty.io</a>
    * @return
    */
  def duplicate: HttpContent = req.duplicate

  /**
    * @see <a href="http://netty.io/5.0/api/io/netty/util/ReferenceCounted.html#refCnt()">netty.io</a>
    * @return
    */
  def refCnt: Int = req.refCnt

  /**
    * @see <a href="http://netty.io/5.0/api/io/netty/util/ReferenceCounted.html#release()">netty.io</a>
    * @return
    */
  def release: Boolean = req.release

  /**
    * @see <a href="http://netty.io/5.0/api/io/netty/util/ReferenceCounted.html#release(int)">netty.io</a>
    * @return
    */
  def release(decrement: Int): Boolean = req.release(decrement)

  /**
    * @see <a href="http://netty.io/5.0/api/io/netty/util/ReferenceCounted.html#retain()">netty.io</a>
    * @return
    */
  def retain: Request = new Request(verb, req.retain, route, dataFactory, this.error)

  /**
    * @see <a href="http://netty.io/5.0/api/io/netty/util/ReferenceCounted.html#retain(int)">netty.io</a>
    * @return
    */
  def retain(decrement: Int): Request = new Request(verb, req.retain(decrement), route, dataFactory, this.error)

  /**
    * @see <a href="http://netty.io/5.0/api/io/netty/handler/codec/http/LastHttpContent.html#trailingHeaders()">netty.io</a>
    * @return
    */
  def trailingHeaders: HttpHeaders = req.trailingHeaders

  /**
    * @see <a href="http://netty.io/5.0/api/io/netty/handler/codec/http/HttpRequest.html#getMethod()">netty.io</a>
    * @return
    */
  def getMethod: HttpMethod = req.getMethod

  /**
    * @see <a href="http://netty.io/5.0/api/io/netty/handler/codec/http/HttpRequest.html#getMethod()">netty.io</a>
    * @return
    */
  def method: HttpMethod = req.getMethod

  /**
    * @see <a href="http://netty.io/5.0/api/io/netty/handler/codec/http/HttpRequest.html#getUri()">netty.io</a>
    * @return
    */
  def getUri: String = req.getUri

  /**
    * The path portion of the request uri (the query string is removed)
    *
    * @return
    */
  def uri: String = {
    val fullUri = req.getUri
    val qndx = fullUri.lastIndexOf("?")
    if (qndx < 0) fullUri
    else fullUri.substring(0, qndx)
  }

  /**
    * @see <a href="http://netty.io/5.0/api/io/netty/handler/codec/http/HttpMessage.html#getProtocolVersion()">netty.io</a>
    * @return
    */
  def getProtocolVersion: HttpVersion = req.getProtocolVersion

  /**
    * @see <a href="http://netty.io/5.0/api/io/netty/handler/codec/http/HttpMessage.html#headers()">netty.io</a>
    * @return
    */
  def headers: HttpHeaders = req.headers

  /**
    * Create a string describing this request, including header, content, and route information.
    *
    * @return
    */
  override def toString: String = {
    s"${req.toString}\nRoute: ${route.toString}"
  }

  /**
    * Retrieve a request attribute by key.
    *
    * @param key
    * @tparam T
    * @return
    */
  def apply[T](key: String):T = attr(key).asInstanceOf[T]

  /**
    * Set a request attribute key-value pair.
    *
    * @param key
    * @param value
    */
  def update(key: String, value: Any):Unit = {
    attr(key) = value
  }

  /**
    * Retrieve a uri parameter value by name.
    *
    * @param key
    * @return
    */
  def / (key: String): String = uriParameters(key)

  /**
    * Execute the next handler in the route.
    *
    * @return the response
    */
  def next: Future[Response] = {
    routeCursor += 1
    val nextHandler = route(routeCursor)
    uriParameters = extractValues(uri, nextHandler.uriPattern)
    nextHandler.callBack(this)
  }
}
