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

@Sharable class HttpRouteMiddlewareRequestHandler
  extends SimpleChannelInboundHandler[FullHttpRequest] {

  import HttpRouteMiddlewareRequestHandler._
  import scala.concurrent._
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
  import scala.util.{Try, Success, Failure}
  import org.microsauce.scetty.util.Error

  val uriRouters = new ListBuffer[Router]
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
  
  def add(router:Router):Unit = uriRouters += router

  def first(router:Router):Unit = uriRouters.insert(0,router)

  /**
   * Responding with status 404.  This handler is the final handler in every route.
   */
  val fileNotFound:Request=>Future[Response] = { req =>
    new Response(
      HttpResponseStatus.NOT_FOUND,
      s"file not found ${req.getUri}",
      "text/plain").toFuture
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

  override def channelRead0(ctx: ChannelHandlerContext, /*message:Any*/request: FullHttpRequest) {
    val verb = request.getMethod.toString match {
      case "GET" => GET
      case "POST" => POST
      case "PUT" => PUT
      case "DELETE" => DELETE
      case _ => GET // default
    }

    val route = getRoute(verb, request.getUri,false)

    val tryFutureResponse = execRoute(new Request(verb, request, route, dataFactory))
    sendHttpResponse(verb,tryFutureResponse,ctx,request)
  }

  private def sendHttpResponse(verb:HttpVerb, tryFutureResponse:Try[Future[Response]],ctx:ChannelHandlerContext,request:FullHttpRequest) {
    tryFutureResponse match {
      case Success(futureResponse) => for ( response <- futureResponse ) {
        writeResponse(ctx,response)
      }
      case Failure(err) =>
        import org.microsauce.scetty.Router._
//        val error = new Error(err)
        logger.severe(err.toString)
        err.printStackTrace()
        val errorRoute = getRoute(GET,"/error",true)
        val errorRequest = new Request(verb, request, errorRoute, dataFactory)
        //errorRequest("_error") = error
        errorRequest.error = err
        val res = new Response(HttpResponseStatus.INTERNAL_SERVER_ERROR, s"internal server error: ${err.getMessage} - ${err.stackTrace}","text/plain")
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
  }

}
