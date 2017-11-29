package org.microsauce.scetty

import org.junit.runner.RunWith
import org.microsauce.scetty.testutils.TestUtils
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaj.http._


@RunWith(classOf[JUnitRunner])
class ScettySpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  val portReservation = TestUtils.reservePort
  val scettyPort = portReservation.releasePort
  val fixture: Scetty = new Scetty()
    .addRouter(new TestRouter)
    .port(scettyPort)

  override def beforeAll() {
    val futureStart = Future {
      fixture.start
    }

    info("wait for server startup . . .")
    Thread.sleep(5000)
    info(s"server listening on port [$scettyPort]")
  }

  override def afterAll() {
    fixture.stop
  }

  "a GET request with uri parameter" should "correctly parse the uri parameter and insert it into the response" in {
    val response = Http(s"http://localhost:$scettyPort/get/it/by/steve").asString

    response.body should be ("hello steve")
  }

  "a POST request with json payload" should "should be deserialized and reserialized" in {
    val theStuff = """{"aString":"myString","anInt":7,"aList":["one","two","three"]}"""
    val response = Http(s"http://localhost:$scettyPort/the/stuff")
        .header("Content-Type", "application/json")
      .postData(theStuff).asString

    info(s"actual: ${response.body}")
    info(s"expected: $theStuff")
    response.body should be (theStuff)
  }

  class TestRouter extends DefaultRouter {

    get("/get/it/by/:name") { req =>
      var name = req / "name"
      OK(s"hello $name").toFuture
    }

    post("/the/stuff") { req =>
      val theStuff = req.json[TheStuff]

      OK(json(theStuff)).toFuture
    }

  }

  case class TheStuff(val aString: String, val anInt: Int, val aList:List[String])

}
