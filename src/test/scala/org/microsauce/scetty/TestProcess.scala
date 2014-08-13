//package org.microsauce.scetty

/**
 * Created by johboon on 7/23/2014.
 */
object TestProcess extends App {

  import scala.sys.process._
  import scala.concurrent._
  import scala.concurrent.ExecutionContext.Implicits.global

  println("executing command")
  future { "C:/servers/liferay-portal-6.0.6/tomcat-6.0.29/bin/catalina.bat run".! }
  println("command under way")
  Thread.sleep(100000)
}
