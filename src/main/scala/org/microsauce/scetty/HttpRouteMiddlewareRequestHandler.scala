package org.microsauce.scetty

import java.util.logging.Logger

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelFuture, ChannelFutureListener, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory
import io.netty.handler.codec.http.{FullHttpRequest, HttpResponseStatus}

import scala.collection.mutable.ListBuffer

object HttpRouteMiddlewareRequestHandler {
  val logger = Logger.getLogger(HttpRouteMiddlewareRequestHandler.getClass.toString)
}

@Sharable
class HttpRouteMiddlewareRequestHandler(val dataFactoryMinSize: Long)
  extends SimpleChannelInboundHandler[FullHttpRequest] {

  import HttpRouteMiddlewareRequestHandler._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent._
  import scala.util.{Failure, Success, Try}

  val uriRouters = new ListBuffer[Router]
  val dataFactory = new DefaultHttpDataFactory(dataFactoryMinSize)

  private implicit def makeChannelFutureListener(listener: (ChannelFuture) => Unit) =
    new ChannelFutureListener {
      override def operationComplete(channel: ChannelFuture) {
        listener(channel)
      }
    }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = ctx.flush()

  def add(router: Router): Unit = uriRouters += router

  def first(router: Router): Unit = uriRouters.insert(0, router)

  /**
    * Respond with status 404.  This handler is the final handler in every route.
    */
  val fileNotFound: Request => Future[Response] = { req =>
    new Response(
      HttpResponseStatus.NOT_FOUND,
      s"file not found ${req.getUri}",
      "text/plain").toFuture
  }

  def getRoute(verb: HttpVerb, uri: String, isError: Boolean) = {
    val assembledRoute = new ListBuffer[HttpRequestHandler]
    for (thisRouter <- uriRouters) {
      assembledRoute ++= thisRouter.getUse(uri)
    }

    for (thisRouter <- uriRouters) {
      assembledRoute ++= thisRouter.getRoute(verb, uri)
    }

    // add the static resource handlers last
    for (thisRouter <- uriRouters) {
      assembledRoute += new HttpRequestHandler(
        GET,
        new UriPattern("*","""/.*""".r, List()), thisRouter.handleStaticResource)
    }
    if (isError) assembledRoute
    else assembledRoute += new HttpRequestHandler(GET, new UriPattern("*","""/.*""".r, List()), fileNotFound)
  }

  override def channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest): Unit =
    doChannelRead0(ctx, request, null)

  private def doChannelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest, error: Throwable): Unit = {
    val verb = request.getMethod.toString match {
      case GET.name => GET
      case POST.name => POST
      case PUT.name => PUT
      case DELETE.name => DELETE
      case _ => GET // default
    }

    val route = getRoute(verb, uriPath(request.getUri), false)

    executeRoute(new Request(verb, request, route, dataFactory, error)) match {
      case Success(futureResponse) => for (response <- futureResponse) {
        writeResponse(ctx, response)
      }

      case Failure(err) => handleError(ctx, request, err)
    }
  }

  private def closeChannel(channel:ChannelFuture):Unit = {
    channel.channel().close()
  }

  private def executeRoute(request: Request): Try[Future[Response]] = {
    try {
      val futureResponse = request.next

      Success(futureResponse)
    } catch {
      case err: Throwable => Failure(err)
    }
  }

  private def handleError(ctx: ChannelHandlerContext, request: FullHttpRequest, err: Throwable): Unit = {
    logger.severe(err.toString)
    err.printStackTrace()

    val errorRequest = request.copy()
    errorRequest.setMethod(io.netty.handler.codec.http.HttpMethod.GET);
    errorRequest.setUri("/error")

    try {

      doChannelRead0(ctx, errorRequest, err) // forward to error page

    } catch {
      case fwdErr: Throwable =>

        import org.microsauce.scetty.implicits._

        val res = new Response(
          HttpResponseStatus.INTERNAL_SERVER_ERROR,
          s"internal server error: ${err.getMessage()} - ${err.stackTrace}\n\nForward error: ${fwdErr.getMessage()} - ${fwdErr.stackTrace}",
          "text/plain")

        writeResponse(ctx, res)
    }
  }


  private def uriPath(nettyUri: String): String = {
    val index = nettyUri.lastIndexOf("?")

    if (index < 0) nettyUri
    else nettyUri.substring(0, index)
  }

  private def writeResponse(ctx: ChannelHandlerContext, response: Response) = {
    ctx.write(response.nettyResponse)
    ctx.writeAndFlush(response.toNettySource).addListener(closeChannel)
  }

}
