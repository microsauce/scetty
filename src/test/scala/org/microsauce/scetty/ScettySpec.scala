package org.microsauce.scetty

import org.junit.runner.RunWith
import org.microsauce.scetty.testutils.TestUtils
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaj.http._


@RunWith(classOf[JUnitRunner])
class ScettySpec extends FlatSpec with Matchers with BeforeAndAfterAll {

  val logMessages = ListBuffer[String]()

  val testRouter = new TestRouter(logMessages)
  val anotherTestRouter = new AntherRouter(logMessages)
  val portReservation = TestUtils.reservePort
  val scettyPort = portReservation.releasePort

  val fixture: Scetty = new Scetty()
    .addRouter(testRouter)
    .addRouter(anotherTestRouter)
    .port(scettyPort)

  override def beforeAll() {
    Future {
      fixture.start
    }

    info("wait for server startup . . .")
    Thread.sleep(1000)
    info(s"server listening on port [$scettyPort]")
  }

  override def afterAll() {
    fixture.stop
  }

  "GET /get/it/by/steve/and/steverson/from/there" should "correctly parse the uri parameters and insert into the response" in {
    val response = Http(s"http://localhost:$scettyPort/get/it/by/steve/and/steverson/from/there").asString

    response.code should be (200)
    response.headers("Content-Type")(0) should be ("text/html")
    response.body should be ("hello steve steverson from there")
  }

  "POST /the/stuff" should "deserialize the request body and reserialize it in the response" in {
    val theStuff = """{"aString":"myString","anInt":7,"aList":["one","two","three"]}"""
    val response = Http(s"http://localhost:$scettyPort/the/stuff")
        .header("Content-Type", "application/json")
      .postData(theStuff).asString

    response.code should be (200)
    response.headers("Content-Type")(0) should be ("application/json")
    response.body should be (theStuff)
  }

  "GET /do/wah/diddy/diddy" should "invoke middleware" in {
    val response = Http(s"http://localhost:$scettyPort/do/wah/diddy/diddy").asString

    logMessages should be (ListBuffer("do => wah/diddy/diddy","another message"))

    response.code should be (200)
    response.headers("Content-Type")(0) should be ("text/html")
    response.body should be (">>dum-diddy-do<<")
  }

  class TestRouter(val logMessages: ListBuffer[String]) extends DefaultRouter {

    get("/get/it/by/:firstName/and/:lastName/from/*") { req =>
      val firstName = req / "firstName"
      val lastName = req / "lastName"
      val from = req / "*_0"

      OK(s"hello $firstName $lastName from $from").toFuture
    }

    post("/the/stuff") { req =>
      val theStuff = req.json[TheStuff]

      OK(json(theStuff)).toFuture
    }

    use("/do/*") { req =>
      val doWhat = req / "*_0"

      testLogInfo(s"do => $doWhat")

      req.next
    }

    use("/do/wah/*") { req =>
      val futureResponse = req.next

      for (response <- futureResponse) {
        response.source = if (response.source.isInstanceOf[String]) {
          s">>${response.source.asInstanceOf[String]}<<"
        } else {
          response.source
        }
      }

      futureResponse
    }

    get("/do/you/know/what/time/it/is") { _ =>
      OK("it is 7").toFuture
    }

    put("/do/something/with/this") { req =>
      val theStuff = req.json[TheStuff]
      theStuff.aString = "not your string anymore"

      Future {
        OK(theStuff.aString, "text/plain")
      }
    }

    get("/do/wah/diddy/diddy") { req =>
      OK("dum-diddy-do").toFuture
    }

    def testLogInfo(message:String): Unit = {
      logMessages += message
    }
  }

  class AntherRouter(val logMessages:ListBuffer[String]) extends DefaultRouter {

    use("/do/wah/diddy*") { req =>
      logMessages += "another message"

      req.next
    }

  }

  case class TheStuff(var aString: String, val anInt: Int, val aList:List[String])

}
