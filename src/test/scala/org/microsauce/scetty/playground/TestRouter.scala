package org.microsauce.scetty.playground

import org.microsauce.scetty.{DefaultRouter, Scetty}

class TestRouter extends DefaultRouter {

  get("/get/it/by/:name") { req =>
    var name = req / "name"
    OK(s"hello $name").toFuture
  }

  get("/get/it/byanudder/:name") { req =>
    var name = req / "name"
    OK(s"hello $name").toFuture
  }

  post("/the/stuff") { req =>
    val theStuff = req.json[TheStuff]

    OK(json(theStuff)).toFuture
  }

}

case class TheStuff(val aString: String, val anInt: Int, val aList:List[String])

object TestTestApp extends App {

  new Scetty()
      .addRouter(new TestRouter)
      .port(8888)
      .start

}