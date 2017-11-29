package org.microsauce.scetty.util

object ScettyUtil {

  def coalesce[T](default:T, values:T*):T = {
    values.filter {
      value => value != null
    } collectFirst { case value => value } match {
      case Some(value) => value
      case None => default
    }
  }

}
