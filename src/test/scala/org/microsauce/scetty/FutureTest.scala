package org.microsauce.scetty

/**
 * Created by jboone on 6/18/2014.
 */
object FutureTest extends SimpleScettyApp {
  import scala.concurrent._
  import scala.concurrent.ExecutionContext.Implicits.global

  case class Data(someData:String)

  def getFutureData = future {
    Thread.sleep(1000)
    new Data("here's some data")
  }

  get("/some/data") { req =>
    val futureData = getFutureData
    for ( data <- futureData )
      yield OK(s"here is your data: ${data.someData}")
  }

  start
}
