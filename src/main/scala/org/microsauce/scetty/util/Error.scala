package org.microsauce.scetty.util

import java.net.InetAddress
import java.io.{PrintWriter, StringWriter}

/**
 * Scetty error class.
 */
class Error(throwable:Throwable) {

  val message = throwable.getMessage
  val stringWriter = new StringWriter()
  val printWriter = new PrintWriter(stringWriter)
  val stackTrace = stringWriter.toString()

  override def toString = {
    s"$errorCode"
  }

  private def errorCode = {
    def addr = InetAddress.getLocalHost();
    val ipAddr = addr.getAddress();
    val hostName = addr.getHostName()
    s"${hostName}-${System.currentTimeMillis()}"
  }
}
