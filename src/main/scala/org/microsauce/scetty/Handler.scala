package org.microsauce.scetty

import scala.concurrent.Future

/**
 * This class models a user defined HTTP request handler.
 */
sealed abstract class Handler {
  val verb: HttpVerb
  val uriPattern: UriPattern

  override def toString = {
    s"[method: ${verb} - $uriPattern]"
  }
}

case class HttpRequestHandler(
  verb: HttpVerb,
  uriPattern: UriPattern,
  callBack : Request=>Future[Response]
) extends Handler
