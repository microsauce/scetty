package org.microsauce.scetty

import scala.collection.mutable.ListBuffer

/**
 * Created by jboone on 3/15/14.
 */
sealed trait Route

case class HttpRoute(handlers:ListBuffer[HttpRequestHandler]) extends Route
case class WSRoute(handlers:ListBuffer[WebSocketHandler]) extends Route

