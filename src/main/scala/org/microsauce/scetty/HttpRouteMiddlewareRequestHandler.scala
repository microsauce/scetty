package org.microsauce.scetty

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus, FullHttpRequest}
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory
import io.netty.channel.ChannelFuture
import scala.collection.mutable.ListBuffer
import java.util.logging.Logger
import io.netty.handler.codec.http.websocketx._

object HttpRouteMiddlewareRequestHandler {
  val logger = Logger.getLogger(HttpRouteMiddlewareRequestHandler.getClass.toString)
}

/**
 * TODO
 *   we can remove @Sharable if we pass the router list in
 */
@Sharable
class HttpRouteMiddlewareRequestHandler /*(
  val uriRouters:ListBuffer[Router],
  val socketRouters:ListBuffer[WebSocketRouter],
  val dataFactory:DefaultHttpDataFactory
)*/ extends SimpleChannelInboundHandler[/*Any]*/FullHttpRequest] {   // TODO change to Any - remove sharable

  import HttpRouteMiddlewareRequestHandler._
  import scala.concurrent._
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  import scala.util.{Try, Success, Failure}
  import org.microsauce.scetty.util.Error

  // websocket data
//  private val stringBuffer = new StringBuilder()
//  private var byteBuffer:ListBuffer[Byte] = new ListBuffer[Byte]
//  private var handShaker:WebSocketServerHandshaker = null
//  private var wsRoute:ListBuffer[WebSocketHandler] = null
//  private var httpHandshakeRequest:FullHttpRequest = null

  val uriRouters = new ListBuffer[Router]
  val socketRouters = new ListBuffer[WebSocketRouter]
  // TODO make configurable - Server/Netty config
  val dataFactory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE)

  private implicit def makeChannelFutureListener(listener: (ChannelFuture) => Unit) =
    new ChannelFutureListener {
      override def operationComplete(channel: ChannelFuture) { listener(channel) }
    }

  override def channelReadComplete(ctx: ChannelHandlerContext):Unit = ctx.flush()

  private val closeChannel:ChannelFuture=>Unit = { channel =>
    channel.channel().close()
  }

  private def execRoute(request:Request):Try[Future[Response]] = {
    try {
	    val futureResponse = request.next
	    Success(futureResponse)
    } catch {
      case err:Throwable => Failure(err)
    }
  }
  
  def add(router:BaseRouter):Unit = router match {
    case rtr : Router => uriRouters += rtr
    case wsRtr : WebSocketRouter => socketRouters += wsRtr
  }

  /**
   * Responding with status 404.  This handler is the final handler in every route.
   */
  val fileNotFound:Request=>Future[Response] = { req =>
    new Response(
      HttpResponseStatus.NOT_FOUND,
      s"file not found ${req.getUri}",
      "text/plain").toFuture
  }

  def getWsRoute(verb:HttpVerb, uri:String) = {
    val assembledRoute = new ListBuffer[WebSocketHandler]
    for ( thisRouter <- socketRouters ) {
      assembledRoute ++= thisRouter.getWSRoute(uri)
    }
    assembledRoute
  }

  def getRoute(verb: HttpVerb, uri: String, isError:Boolean) = {
    val assembledRoute = new ListBuffer[HttpRequestHandler]
    for ( thisRouter <- uriRouters ) {
      assembledRoute++=thisRouter.getRoute(verb, uri)
    }
    // add the static resource handlers last
    for ( thisRouter <- uriRouters ) {
      assembledRoute += new HttpRequestHandler(
        GET,
        new UriPattern("*","""/.*""".r, List()),thisRouter.handleStaticResource)
    }
    if (isError) assembledRoute
    else assembledRoute += new HttpRequestHandler(GET,new UriPattern("*","""/.*""".r, List()),fileNotFound)
  }

  // TODO inject handlers (http/websocket) rather than match case
  // TODO sealed case class Handler
  // case class HttpRequestHandler ext Handler
  // case class WSHandler ext Handler

  override def channelRead0(ctx: ChannelHandlerContext, /*message:Any*/request: FullHttpRequest) {
//    message match {
//      case request : FullHttpRequest =>
        val verb = request.getMethod.toString match {
          case "GET" => GET
          case "POST" => POST
          case "PUT" => PUT
          case "DELETE" => DELETE
          case _ => GET // default
        }

//        val wsRoute = getWsRoute(verb,request.getUri)
//        if ( wsRoute.size > 0 ) {
//          handShake(ctx,request,wsRoute)
//        } else {
          val route = getRoute(verb, request.getUri,false)

          val tryFutureResponse = execRoute(new Request(verb, request, route, dataFactory))
          sendHttpResponse(verb,tryFutureResponse,ctx,request)
//        }

//      case close : CloseWebSocketFrame =>
//        handShaker.close(ctx.channel(), close.retain)

//      case frame: WebSocketFrame =>
//        handleSocketFrame(ctx,frame)
//    }
  }

//  private def handShake(ctx:ChannelHandlerContext,httpRequest:FullHttpRequest,wsRoute:ListBuffer[WebSocketHandler]) {
//    import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory._
//
//    this.wsRoute = wsRoute
//    this.httpHandshakeRequest = httpRequest
//
////    // TODO channel registration ???   uri => channel
////    ChannelRegistry.register(httpRequest.getUri, ctx.channel)
//
//    val wsFactory = new WebSocketServerHandshakerFactory(
//      s"ws://${httpRequest.headers.get(HttpHeaders.Names.HOST)}${httpRequest.getUri}", null, false)
//    handShaker = wsFactory.newHandshaker(httpRequest)
//    if (handShaker == null) sendUnsupportedWebSocketVersionResponse(ctx.channel)
//    else handShaker.handshake(ctx.channel, httpRequest)
//  }

//  private def execWSRoute(rawMessage:Any) = {
//    var message = rawMessage
//    for ( handler <- wsRoute ) {
//      message = handler.callBack(message)
//    }
//    message
//  }

  private def sendHttpResponse(verb:HttpVerb, tryFutureResponse:Try[Future[Response]],ctx:ChannelHandlerContext,request:FullHttpRequest) {
    tryFutureResponse match {
      case Success(futureResponse) => for ( response <- futureResponse ) {
        writeResponse(ctx,response)
      }
      case Failure(err) =>
        val error = new Error(err)
        logger.severe(error.toString)
        err.printStackTrace()
        val errorRoute = getRoute(GET,"/error",true)
        val errorRequest = new Request(verb, request, errorRoute, dataFactory)
        errorRequest("_error") = error
        val res = new Response(HttpResponseStatus.INTERNAL_SERVER_ERROR, s"internal server error: ${err.getMessage} - ${error.toString}","text/plain")
        if ( errorRoute.size > 0 ) {
          val errorTryFutureResponse = execRoute(errorRequest)
          errorTryFutureResponse match {
            case Success(futureErrorResponse) => for (errorResponse <- futureErrorResponse) {
              writeResponse(ctx,errorResponse)
            }
            case Failure(errFail) =>
              logger.severe("Error occurred while rendering the error page")
              errFail.printStackTrace
              writeResponse(ctx,res)
          }
        } else {
          writeResponse(ctx,res)
        }
    }
  }

  private def writeResponse(ctx:ChannelHandlerContext,response:Response)= {
      ctx.write(response.nettyResponse)
      ctx.writeAndFlush(response.nettySource).addListener(closeChannel)
//    response.write(ctx.channel)
//    ctx.channel.write(response.nettyResponse)
//    ctx.channel.writeAndFlush(response.nettySource).addListener(closeChannel)
  }

//  private def handleSocketFrame(ctx:ChannelHandlerContext,socketFrame:WebSocketFrame):Unit = {
//    val messageComplete = socketFrame.isFinalFragment
//    var isBinary = false
//    val incomingMessage = socketFrame match {
//      case textFrame:TextWebSocketFrame =>
//        stringBuffer append textFrame.text
//        if ( messageComplete ) stringBuffer.toString
//        else null
//
//      case binaryFrame:BinaryWebSocketFrame =>
//        byteBuffer ++= binaryFrame.content.array
//        if ( binaryFrame.isFinalFragment ) byteBuffer.toArray
//        else null
//
//      case continuationFrame:ContinuationWebSocketFrame =>
//        if ( continuationFrame.isFinalFragment ) {
//          if ( continuationFrame.aggregatedText != null ) {
//            execWSRoute(continuationFrame.aggregatedText)
//          } else {
//            val messageContent = new Array[Byte](continuationFrame.content.readableBytes)
//            continuationFrame.content.readBytes(messageContent)
//            execWSRoute(messageContent)
//          }
//        } else null
//    }
//
//    if ( messageComplete ) {
//      stringBuffer.clear
//      byteBuffer.clear
//      val response = execWSRoute(incomingMessage)
//      // TODO send the response   - responseType will be the same as the request (text/binary)
//      // TODO how to handle broadcast - pushing messages to other channels
//    }
//
//  }

}
