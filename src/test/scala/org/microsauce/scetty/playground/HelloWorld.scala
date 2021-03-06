package org.microsauce.scetty.playground

import org.microsauce.scetty.{DefaultRouter, Scetty, ScettyContextBuilder}
import org.microsauce.scetty.Router._
import org.microsauce.scetty.util.Error
import org.microsauce.scetty.implicits._

object HelloWorld extends DefaultRouter with App {

  use(cookieSupport)
  use { req =>
    println(s"\n\n$req\n\n")
    req.next
  }

//  get("/error") { req =>
//    ERR(s"KA-BOOM: ${req.error.stackTrace}","text/plain").toFuture
//  }

  get("/boom/boom") { req =>
    val boom = 1 / 0
    OK("never happen").toFuture
  }

  get("/new/operator/:name") { req =>
    val uname = req/"name"
    val qname = req?"name"|"StevenQ"
    val age = (req?"age"|"0").toInt
    OK(s"uname: $uname - qname: $qname - age: ${age+7}").toFuture
  }

  use("/a/*") { req =>
    val dog = Dog("Stu",12)
    println(s"cache a dog in the request: $dog")
    println(s"do rest ${req/"*_0"}")
    req("aDog") = dog
    req.next
  }

  get("/a/dog") { req =>
    val aDog = req[Dog]("aDog")
    OK(s"<i>lookey what I just pulled out of the request: $aDog</i>").toFuture
  }

  get("/hello/:name") { req =>
    println(s"request uri: ${req.getUri}")
    OK(s"<b>Hello ${req/"name"}! Love duh default netty router hoo hoo</b>").toFuture
  }

  get("/goodbye/:name") { req =>
    println(s"request uri: ${req.getUri}")
    OK(s"<b>Bye Bye ${req/"name"}! Love duh default netty router hoo hoo</b>").toFuture
  }

  post("/some/json") { req =>
    val dogPost = req.json[Dog]
    OK(s"thanks for posting your dog $dogPost").toFuture
  }

  var age = 1
  get("/some/json") { req =>
    age += 1
    println(s"getting some json: $age")
    OK(json(Dog("Sally",age))).toFuture
  }

  // reward middleware
  use("/grade/*") { req =>
    req("reward") = req/"*_0" match {
      case "A" => "gold star"
      case "B" => "silver star"
      case "C" => "bronze star"
      case "D" | "F" => "dunce cap"
    }
    req.next
  }

  get("/grade/:grade") { req =>
    OK(s"The grade ${req/"grade"} has earned you a ${req("reward")}").toFuture
  }

  new Scetty(ScettyContextBuilder.create
    .addRouter(HelloWorld)
    .inetAddress("localhost")
    .port(8888)
    .build
  ).start
}
