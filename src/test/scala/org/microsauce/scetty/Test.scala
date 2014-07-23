package org.microsauce.scetty

import org.microsauce.scetty.Router._

/**
 * Created by jboone on 6/10/2014.
 */
case class DiggityDog(name:String,bones:Int)
object Test extends SimpleScettyApp {

  get("/hello/:name") { req =>
    OK(s"hey dare ${req/"name"}").toFuture
  }

  post("/dog") { req =>
    val dog = req.json[DiggityDog]
    OK(s"we have your dog ${dog.name}").toFuture
  }

  get ("/dogs") { req =>
    val allMyDogs = List(DiggityDog("Fred",7),DiggityDog("Stu",9))
    OK(json(allMyDogs)).toFuture
  }

  use("/secret/*") { req =>
    req?"password"|"WRONG" match {
      case "supersecret" => req.next
      case _ => FORBIDDEN("Access Denied!!!").toFuture
    }
  }

  get("/secret/stuff") { req =>
    OK("Shh! Don't tell anybody!").toFuture
  }

  start
}


