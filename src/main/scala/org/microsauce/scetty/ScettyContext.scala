package org.microsauce.scetty

import scala.collection.mutable.ListBuffer

class ScettyContext {

  var inetAddress: String = _
  var port: Int = _
  var maxInitialLineLength: Int = _
  var maxHeaderSize: Int = _
  var maxChunkSize: Int = _
  var maxContentLength: Int = _
  var dataFactoryMinSize: Long = _

  var ssl: Boolean = _
  var sslKeystore: String = _
  var keypass: String = _

  var routers: ListBuffer[Router] = _
}
