package org.microsauce.scetty

import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory

object ScettyDefaults {

  val inetAddressName: String = "localhost";
  val port: Int = 80
  val maxInitialLineLength: Int = 4096
  val maxHeaderSize: Int = 8192
  val maxChunkSize: Int = 8192
  val maxContentLength: Int = 65536
  val dataFactoryMinSize:Long = DefaultHttpDataFactory.MINSIZE

  val ssl: Boolean = false
  val sslKeystore: String = ""
  val keypass: String = ""
}
