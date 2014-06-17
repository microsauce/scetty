package org.microsauce.scetty

import java.io.{PrintWriter, StringWriter, File}
import scala.concurrent._
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.HttpHeaders.Names._
import org.microsauce.scetty.coder.Coder

/**
 * The Router companion object defines default values, commonly used middleware handlers, and implicit
 * extension classes
 */
object Router {

  import scala.collection.mutable._
  import ExecutionContext.Implicits.global

  private val CHUNK_LEN = 4000 // bytes - TODO make configurable
  private val SESSION_ID = "jsession_"

  def docRoot = System.getProperty("user.dir") + "/docs"
  def templateRoot = System.getProperty("user.dir") + "/templates"

  /**
   * A middleware handler that adds cookie support to an application
   */
  val cookieSupport:Request=>Future[Response] = { req =>
    // decode cookies
    val cookieHeader = req.headers.get("Cookie")
    if ( cookieHeader != null ) {
      val cookies = CookieDecoder.decode(cookieHeader)
      val iterator = cookies.iterator
      while (iterator.hasNext) {
        val cookie = iterator.next
        req.cookies(cookie.getName) = cookie
      }
    }
    
    val futureResponse = req.next
    
    // encode cookies
    for ( 
      response <- futureResponse;
      cookie <- response.getCookies.values
    ) {
      response.nettyResponse.headers.add("Set-Cookie", ServerCookieEncoder.encode(cookie))
    }
    
    futureResponse
  }

//  def sessionSupport:Request=>Future[Response] = {
//    sessionSupport(20*60,coder)  // TODO do nothing coder
//  }

  def sessionSupport(coder:Coder[Array[Byte],Array[Byte]]):Request=>Future[Response] = {
    sessionSupport(20*60,coder)
  }

  /**
   * Construct a middleware handler that adds session support to an application.
   * @param coder a composable coder instance
   * @return
   */
  def sessionSupport(timeout:Int,coder:Coder[Array[Byte],Array[Byte]]):Request=>Future[Response] = {
    import scala.pickling._
    import binary._

    { req =>
      // decode session
      val serializedSession = assembledSession(req.cookies)
      if ( serializedSession.length > 0 ) {
        val clearData = coder.decode(serializedSession.b)
        req.sess ++= BinaryPickle(clearData).unpickle[Map[String,Array[Byte]]]
      }

      val futureResponse = req.next

      // encode session
      val pickledSession = req.sess.pickle.value
      val encryptedSession = coder.encode(pickledSession)

      // set the session cookie
      for ( response <- futureResponse ) {
        val sessionCookies = chunkedSession(encryptedSession)
        sessionCookies.foreach { cookie:Cookie =>
          cookie.setMaxAge(timeout)
          response.nettyResponse.headers.add("Set-Cookie",ServerCookieEncoder.encode(cookie))
        }
      }

      futureResponse
    }
  }

  private def chunkedSession(session:Array[Byte]) = {
    val chunks = chunkedSessionBytes(session)
    val cookies = new scala.collection.mutable.ListBuffer[Cookie]()
    var ndx = 0
    for ( c<-chunks ) {
      val cookie = new DefaultCookie(SESSION_ID+ndx, c.s)
      cookie.setPath("/")
      cookies += cookie
      ndx+=1
    }
    cookies
  }

  private def assembledSession(cookies:Map[String,Cookie]) = {
    val buffer = new StringBuilder()

    var ndx=0
    var c = cookies.getOrElse(SESSION_ID+ndx,null)
    while( c != null ) {
      buffer.append(c.getValue)
      ndx+=1
      c = cookies.getOrElse(SESSION_ID+ndx,null)
    }
    buffer.toString
  }

  private def chunkedSessionBytes(session:Array[Byte]) = {
    val sessLen = session.length
    val quotient = session.length.toDouble / CHUNK_LEN.toDouble
    val chunks = Math.ceil(quotient).toInt
    val cookies = new Array[Array[Byte]](chunks)
    var start,end=0
    var accumulatedBytes = 0
    for ( ndx <- 0 until chunks ) {
      end = if ( sessLen-accumulatedBytes<CHUNK_LEN ) sessLen
      else start+CHUNK_LEN
      cookies(ndx) = session.slice(start,end)
      start = end
      accumulatedBytes+=CHUNK_LEN
    }
    cookies
  }

  private def assembleSessionBytes(chunks:Array[Array[Byte]]) = {
    val len = chunks.foldLeft(0){(total,arr)=>total+arr.length}
    var accumulatedBytes = 0
    val session = new Array[Byte](len)
    var start = 0
    var end = 0
    for ( c<-chunks ) {
      end = if ( len-accumulatedBytes<CHUNK_LEN ) len
      else start+CHUNK_LEN
      c.copyToArray(session,start,end)
      start = end
      accumulatedBytes+=CHUNK_LEN
    }
    session
  }

  private def chunkedSessionString(session:String) = {
    val sessLen = session.length
    val quotient = session.length.toDouble / CHUNK_LEN.toDouble
    val chunks = Math.ceil(quotient).toInt
    val cookies = new Array[String](chunks)//new Array[Array[Byte]](chunks)
    var start,end=0
    var accumulatedBytes = 0
    for ( ndx <- 0 until chunks ) {
      end = if ( sessLen-accumulatedBytes<CHUNK_LEN ) sessLen
      else start+CHUNK_LEN
      cookies(ndx) = session.substring(start,end)// slice(start,end)
      start = end
      accumulatedBytes+=CHUNK_LEN
    }
    cookies
  }

  private def assembleSessionString(chunks:Array[String]) = {
    val session = new StringBuilder()//new Array[Byte](len)
    for ( c<-chunks ) {
      session.append(c)
    }
    session.toString
  }

  class Stats extends DefaultRouter // TODO

  implicit def StringToArrayByte(str:String) = str.getBytes("utf-8")
  implicit class StringB(str:String) {
    def b = str.getBytes("utf-8")
  }
  implicit class ByteS(bytes:Array[Byte]) {
    def s = new String(bytes, "utf-8")
  }

  /**
   * This implicit class provides the following al
   * @param opt
   * @tparam A
   */
  implicit class TerseOption[A](opt:Option[A]) {
    def |(any:A) = if (opt.isEmpty) any else opt.get
  }

  implicit class StackTraceString(t:Throwable) {
    def stackTrace = {
      val stringWriter = new StringWriter()
      val printWriter = new PrintWriter(stringWriter)
      t.printStackTrace(printWriter)
      stringWriter.toString
    }
  }
}

/**
 *
 */
trait Router extends BaseRouter {

  import org.fusesource.scalate._
  import scala.concurrent._
  import UriUtils._

  protected val documentRoot:String
  protected val templateRoot:String

  protected override val uriHandlers = scala.collection.mutable.ListBuffer[/*Handler*/HttpRequestHandler]()
  lazy protected val templateFolder = new File(templateRoot)
  lazy protected val templateEngine = createScalateEngine

  /**
   * A handler which responds with the static resource matching the request uri.  This
   * handler the penultimate handler in every route.
   */
  val handleStaticResource:Request=>Future[Response] = { req =>
    val file = new File(documentRoot+req.getUri)
    if ( file.exists ) OK(file).toFuture
    else req.next
  }

  private def createScalateEngine = {
    val engine = new TemplateEngine
    engine.workingDirectory = templateFolder
    engine
  }

  /**
   * Render a scalate view.
   * @param templateUri
   * @param params
   * @return
   */
  def scalate(templateUri:String, params:Map[String,Any]) =
    templateEngine.layout(templateUri,params)

  /**
   * Render a view.  By default the template engine is Scalate.
   */
  var render:(String,Map[String,Any])=>String = { (templateUri, params) => scalate(templateUri, params) }

  /**
   * Create a Json object.
   * @param obj a case class, Map, List, or Tuple
   * @return a Json object encapsulating the obj parameter
   */
  def json(obj:AnyRef) = new Json(obj)

  /**
   * Create an OK response with json content.  Response Content-Type header is application/json.
   * @param json response content
   * @return
   */
  def OK(json:Json) = {
    new Response(HttpResponseStatus.OK/*200*/,json.toString,"application/json")
  }

  /**
   * Create an OK response with File content.  Response Content-Type header determined by file extension.
   * @param file response content
   * @return
   */
  def OK(file:File) = {    
    new Response(HttpResponseStatus.OK/*200*/,file,MimeTable.getType(file.getName))
  }

  /**
   * Create an OK Response with String content.  Response Content-Type header defaults to text/html.
   * @param str response content
   * @return
   */
  def OK(str:String):Response = {
    OK(str,"text/html")
  }

  /**
   * Create an OK Response with String content and the given Content-Type response header value.
   * @param str response content
   * @param contentType
   * @return
   */
  def OK(str:String,contentType:String) = {
    new Response(HttpResponseStatus.OK/*200*/,str,contentType)
  }

  /**
   * Create an OK Response with Array[Byte] content.  Response Content-Type header defaults to application/octet-stream.
   * @param bytes response content
   * @return
   */
  def OK(bytes:Array[Byte]):Response = {
    OK(bytes,"application/octet-stream")
  }

  /**
   * Create an OK Response with Array[Byte] content and the given Content-Type response header value.
   * @param bytes response content
   * @param contentType
   * @return
   */
  def OK(bytes:Array[Byte],contentType:String) = {
    new Response(HttpResponseStatus.OK/*200*/,bytes,contentType)
  }

  /**
   * Create an OK Response with ByteBuf content and the given Content-Type response header value.
   * @param buf response content
   * @param contentType
   * @return
   */
  def OK(buf:ByteBuf,contentType:String) = {
    new Response(HttpResponseStatus.OK/*200*/,buf,contentType)
  }

  /**
   * Create an OK Response with a redirect header
   * @param uri response content
   * @return
   */
  def REDIRECT(uri:String) = {
    val response = new Response(HttpResponseStatus.OK,null,"text/html")
    response.nettyResponse.headers.set(LOCATION, uri)
    response
  }

  /**
   * Create an Error response with File content.  Content-Type header value is determined by file extension.
   * @param file response content
   * @return
   */
  def ERR(file:File) = {    
    new Response(HttpResponseStatus.INTERNAL_SERVER_ERROR/*500*/,file,MimeTable.getType(file.getName))
  }

  /**
   * Create an Error response with String content.  Content-Type header value text/html.
   * @param str response content
   * @return
   */
  def ERR(str:String):Response = {
    ERR(str,"text/html")
  }

  /**
   * Create an Error response with String content.
   * @param str response content
   * @param contentType set the value of the Content-Type header
   * @return
   */
  def ERR(str:String,contentType:String) = {
    new Response(HttpResponseStatus.INTERNAL_SERVER_ERROR/*500*/,str,contentType)
  }

  /**
   * Create an Error response with String content.  Content-Type header is application/octet-stream
   * @param bytes response content
   * @return
   */
  def ERR(bytes:Array[Byte]):Response = {
    ERR(bytes,"application/octet-stream")
  }

  /**
   * Create an Error response with String content.
   * @param bytes response content
   * @param contentType set the value of the Content-Type header
   * @return
   */
  def ERR(bytes:Array[Byte],contentType:String) = {
    new Response(HttpResponseStatus.INTERNAL_SERVER_ERROR/*500*/,bytes,contentType)
  }

  def NOT_FOUND(str:String):Response = {
    NOT_FOUND(str,"text/html")
  }

  def NOT_FOUND(str:String,contentType:String) = {
    new Response(HttpResponseStatus.NOT_FOUND/*404*/,str,contentType)
  }

  /**
   * Register a POST request handler.
   * @param uriPattern
   * @param handler a callback function taking a Request parameter and returning a Future[Response]
   * @return Unit
   */
  def post(uriPattern: String)(handler: Request => Future[Response]) {
    addHandler(POST, uriPattern, handler)
  }

  /**
   * Register a PUT request handler.
   * @param uriPattern
   * @param handler a callback function taking a Request parameter and returning a Future[Response]
   * @return Unit
   */
  def put(uriPattern: String)(handler: Request => Future[Response]) {
    addHandler(PUT, uriPattern, handler)
  }

  /**
   * Register a DELETE request handler.
   * @param uriPattern
   * @param handler a callback function taking a Request parameter and returning a Future[Response]
   * @return Unit
   */
  def delete(uriPattern: String)(handler: Request => Future[Response]) {
    addHandler(DELETE, uriPattern, handler)
  }

  /**
   * Register a middleware handler.
   * @param uriPattern
   * @param handler a callback function taking a Request parameter and returning a Future[Response]
   * @return Unit
   */
  def use(uriPattern: String)(handler: Request => Future[Response]) {
    addHandler(USE, uriPattern, handler)
  }

  /**
   * Register a middleware handler.
   * @param handler a callback function taking a Request parameter and returning a Future[Response]
   * @return Unit
   */
  def use(handler: Request => Future[Response]) {
    addHandler(USE, "*", handler)
  }

  protected override def addHandler(verb: HttpVerb, uriString: String, handler: Request => Future[Response]) {
    val uriPattern = parseUriString(uriString)
    uriHandlers += new HttpRequestHandler(verb, uriPattern, handler)
  }

}
