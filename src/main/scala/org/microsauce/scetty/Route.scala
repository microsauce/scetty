package org.microsauce.scetty

import scala.collection.mutable.ListBuffer

case class HttpRoute(handlers:ListBuffer[HttpRequestHandler])


