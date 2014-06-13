package org.microsauce.scetty

/**
 * Created by jboone on 6/10/2014.
 */
case class DiggityDog(name:String)
object Test extends SimpleScettyApp {

  get("/hello/:name") { req =>
    OK(s"hey dare ${req/"name"}").toFuture
  }

  post("/dog") { req =>
    val dog = req.json[DiggityDog]
    OK(s"we have your dog ${dog.name}").toFuture
  }

  start
}

