package org.microsauce.scetty.playground

import org.microsauce.scetty.SimpleScettyApp

object FutureTest extends SimpleScettyApp {
  import scala.concurrent._
  import scala.concurrent.ExecutionContext.Implicits.global

  import ExecutionContext.Implicits.global

  case class Data(someData:String)

  def getFutureData = Future {
    Thread.sleep(1000)
    new Data("here's some data")
  }

  get("/some/data") { req =>
    val futureData = getFutureData
    for ( data <- futureData )
      yield OK(s"here is your data: ${data.someData}")
  }

  get("/some/other/data") { req =>
    val futureData = getFutureData
    for ( data <- futureData )
      yield OK(s"here is your other data: ${data.someData}")
  }

  port(8080).start
}
