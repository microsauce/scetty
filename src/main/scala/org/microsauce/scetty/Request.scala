package org.microsauce.scetty

import io.netty.handler.codec.http.multipart.{DefaultHttpDataFactory, HttpPostRequestDecoder, FileUpload, Attribute}
import io.netty.handler.codec.http.{HttpHeaders, QueryStringDecoder, Cookie, FullHttpRequest}
import scala.collection.mutable.ListBuffer
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder

/**
 * This class decorates the Netty FullHttpRequest providing additional functionality:
 *
 * - uri parameter map
 * - mutable request attribute map
 * - mutable session attribute map
 * - json serializer/deserializer
 * - query string map
 * - form map
 * - cookie map
 * - next method - execute the next handler in the route
 *
 */
class Request(val verb:HttpVerb, val req:FullHttpRequest,val route: ListBuffer[HttpRequestHandler],val dataFactory:DefaultHttpDataFactory) {

  import UriUtils._
  import io.netty.handler.codec.http.HttpHeaders.Names._
  import org.json4s._
  import org.json4s.jackson.Serialization
  import org.json4s.jackson.Serialization.{read, write}

  implicit val formats = Serialization.formats(NoTypeHints)

  private val queryStringDecoder = new QueryStringDecoder(req.getUri)
  private var uriParameters:Map[String,String] = null
  private var cursor = -1
  private val attr = scala.collection.mutable.Map[String,Any]()
  private val postRequestDecoder: HttpPostRequestDecoder = if ( verb == POST || verb == PUT )
    new HttpPostRequestDecoder(dataFactory, req)
  else null

  /**
   * The mutable session map.  All session attribute values are of type Array[Byte].  Use Scala Pickling
   * to cache complex/user-defined types.
   */
  val sess = scala.collection.mutable.Map[String,Array[Byte]]()

  /**
   * The mutable cookie map.
   */
  val cookies = scala.collection.mutable.Map[String,Cookie]()

  /**
   * Retrieve a form parameter value with the given name.
   * @param key
   * @return
   */
  def &(key:String):Option[String] = {
    val data = postRequestDecoder.getBodyHttpData(key)
    data match {
      case x:Attribute => Some(x.getValue)
      case _ => None
    }
    // TODO data ==> Attribute, FileUpload, HttpData
  }
  
  def ^^(key:String) = {
    val data = postRequestDecoder.getBodyHttpData(key)
    data match {
      case x:FileUpload => x.getByteBuf
      case _ => null
    }
  }

  /**
   * Retrieve a query parameter value by key.
   * @param key
   * @return
   */
  def ?(key:String):Option[String] = {
    val value = queryStringDecoder.parameters.get(key)
    if (value != null && value.size > 0) Some(value.get(0)) // TODO return the first element for now
    else None
  }

  /**
   * Deserialize the (json) request body as an object of the given type.
   * @tparam T
   * @return
   */
  def json[T:Manifest] = {
    assert(contentType.startsWith("application/json"),s"Content-Type application/json expected, instead found $contentType")
    val jsonStr = bodyString
    if ( jsonStr != null ) read[T](jsonStr)
    else null
  }

  /**
   * The request body content as a String
   * @return body content
   */
  def bodyString:String = {
    content.toString(java.nio.charset.Charset.forName("utf-8"))
  }

  /**
   * Retrieve the Content-Type header value
   * @return
   */
  def contentType:String = req.headers.get(CONTENT_TYPE)

  /**
   * @see <a href="http://netty.io/5.0/api/io/netty/buffer/ByteBufHolder.html#content()">netty.io</a>
   * @return
   */
  def content:ByteBuf = req.content
  /**
   * @see  <a href="http://netty.io/5.0/api/io/netty/handler/codec/http/FullHttpRequest.html#copy()">netty.io</a>
   * @return
   */
  def copy:Request = new Request(verb, req.copy, route, dataFactory)

  /**
   * @see <a href="http://netty.io/5.0/api/io/netty/handler/codec/http/FullHttpRequest.html#duplicate()">netty.io</a>
   * @return
   */
  def duplicate = req.duplicate

  /**
   * @see <a href="http://netty.io/5.0/api/io/netty/util/ReferenceCounted.html#refCnt()">netty.io</a>
   * @return
   */
  def refCnt:Int = req.refCnt

  /**
   * @see <a href="http://netty.io/5.0/api/io/netty/util/ReferenceCounted.html#release()">netty.io</a>
   * @return
   */
  def release:Boolean = req.release

  /**
   * @see <a href="http://netty.io/5.0/api/io/netty/util/ReferenceCounted.html#release(int)">netty.io</a>
   * @return
   */
  def release(decrement:Int):Boolean = req.release(decrement)

  /**
   * @see <a href="http://netty.io/5.0/api/io/netty/util/ReferenceCounted.html#retain()">netty.io</a>
   * @return
   */
  def retain:Request = new Request(verb, req.retain, route, dataFactory)

  /**
   * @see <a href="http://netty.io/5.0/api/io/netty/util/ReferenceCounted.html#retain(int)">netty.io</a>
   * @return
   */
  def retain(decrement:Int):Request = new Request(verb, req.retain(decrement), route, dataFactory)

  /**
   * @see <a href="http://netty.io/5.0/api/io/netty/handler/codec/http/LastHttpContent.html#trailingHeaders()">netty.io</a>
   * @return
   */
  def trailingHeaders:HttpHeaders = req.trailingHeaders

  /**
   * @see <a href="http://netty.io/5.0/api/io/netty/handler/codec/http/HttpRequest.html#getMethod()">netty.io</a>
   * @return
   */
  def getMethod = req.getMethod

  /**
   * @see <a href="http://netty.io/5.0/api/io/netty/handler/codec/http/HttpRequest.html#getUri()">netty.io</a>
   * @return
   */
  def getUri:String = req.getUri

  /**
   * The path portion of the request uri (the query string is removed)
   * @return
   */
  def uri = {
    val fullUri = req.getUri
    val qndx = fullUri.lastIndexOf("?")
    if ( qndx < 0 )
      fullUri
    else fullUri.substring(0,qndx)
  }

  /**
   * @see <a href="http://netty.io/5.0/api/io/netty/handler/codec/http/HttpMessage.html#getProtocolVersion()">netty.io</a>
   * @return
   */
  def getProtocolVersion = req.getProtocolVersion

  /**
   * @see <a href="http://netty.io/5.0/api/io/netty/handler/codec/http/HttpMessage.html#headers()">netty.io</a>
   * @return
   */
  def headers = req.headers

  /**
   * Create a string describing this request, including header, content, and route information.
   * @return
   */
  override def toString = {
    s"${req.toString}\nRoute: ${route.toString}"
  }

  /**
   * Retrieve a request attribute by key.
   * @param key
   * @tparam T
   * @return
   */
  def apply[T](key:String) = attr(key).asInstanceOf[T]

  /**
   * Set a request attribute key-value pair.
   * @param key
   * @param value
   */
  def update(key:String,value:Any) { attr(key) = value }

  /**
   * Retrieve a uri parameter value by name.
   * @param key
   * @return
   */
  def / (key:String) = uriParameters(key)

  /**
   * Execute the next handler in the route.
   * @return the response
   */
  def next = {
    cursor+=1
    val thisRoute = route(cursor)
    uriParameters = extractValues(uri, thisRoute.uriPattern)
    thisRoute.callBack(this)
  }
}
