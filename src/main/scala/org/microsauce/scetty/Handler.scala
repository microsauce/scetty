package org.microsauce.scetty

import scala.concurrent.Future

/**
 * This class models a user defined HTTP request handler.
 */
sealed abstract class Handler {
  val verb: HttpVerb
  val uriPattern: UriPattern
//  val callBack: Request => Future[Response]

  override def toString = {
    s"[method: ${verb} - $uriPattern]"
  }
}

case class HttpRequestHandler(
  val verb: HttpVerb,
  val uriPattern: UriPattern,
  val callBack : Request=>Future[Response]
) extends Handler

case class WebSocketHandler(
  val verb: HttpVerb,
  val uriPattern: UriPattern,
  val callBack : Any=>Future[Any],
  val endPoint : Boolean
) extends Handler
