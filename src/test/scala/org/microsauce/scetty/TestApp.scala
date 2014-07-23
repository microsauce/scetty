package org.microsauce.scetty
import java.nio.charset.Charset
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import org.apache.commons.codec.Charsets
import org.microsauce.scetty.Router._
import scala.language.existentials
import org.microsauce.scetty.coder.Coder._
import org.microsauce.scetty.coder.Coder


case class Pet(val name:String,age:Int)
case class UserData(val name:String,val age:Int, weight:Double,pets:List[Pet])


object TestApp extends App {

  import org.microsauce.scetty.Router._

  println (s"working directory: ${System.getProperty("user.dir")}")
//  import Transform._
  
  class MyRouter(val documentRoot:String, val templateRoot:String) extends Router {
    
//    use("/json:theRest") { req =>
//      if ( req.contentType.startsWith( "application/json" ) ) req("object") = req.content.array
//    }

    use { req =>
      req.next
    }

    use("/secure/:any") { req =>
      req("any")
      req.next
    }

    get("/impatient/:name") { req =>
      OK(s"${req/"name"} is very impatient :(").toFuture
    }

    // create the request cookie map
    use(cookieSupport)
     
    val myAesCoder: Coder[Array[Byte], Array[Byte]] = aesCoder(
      "C2F8A77D47A177C74F0C266B5063D018", 
      "0DE4245D1278C6BB9E218BC45A72D0B5", 
      "AES/CBC/PKCS5Padding"
    )

    val func = {bytes:Array[Byte]=>bytes}
    val coder = new Coder(func,func)
    use(sessionSupport(base64Coder2.compose(myAesCoder)))
    
    use("/hell:theRest") { req =>
      println(s"hell middleware: ${req / "theRest"}")
      req.next
    }
    
    use("/he:theRest") { req =>
      println(s"he middleware: ${req / "theRest"}")
      req.next
    }
    
    get("/hello/:name") { req =>
      OK(s"<b>hello ${req / "name"}</b>").toFuture
    }
    
    get("/realfuture/:name") { req => future {
        println("before sleep")
        Thread.sleep(2000)
        println("after sleep")
        OK(s"hellooooooooo ${req / "name"}")
      }
    }
    
    post ("/upload/a/binary/file") { req =>
      req ^^ "theFile" match {
        case Some(fileBuf) => OK(fileBuf,"image/png").toFuture
        case None => OK("file not found").toFuture
      }
    }
    
    post ("/upload/a/text/file") { req =>
      req ^^ "theFile" match {
        case Some(fileBuf) => OK(fileBuf.toString(Charsets.UTF_8)).toFuture
        case None => OK("file not found").toFuture
      }
    }
    
    post("/post/data") { req =>
println("POST DATA")      
//      val name = req ## "name"
//      val arr = req.content.
      val contentAsString = req.content.toString(Charset.forName("UTF-8"))
println(s"the json payload: $contentAsString")
      OK(s"post data: $contentAsString").toFuture
    }
    
    post("/post/data2") { req =>
      println("POST DATA 2")      
      val name = req&"name"|"Jimbo Jones"
      val age = req&"age"|"20"
      OK(s"post data: $name - age: $age").toFuture
    }
    
    var counter=0
    get("/render/a/view") { req =>
      counter+=1
      OK(render("myview.jade", 
        Map(
          "name"->("Foo","Bar"),
          "city"->"Madtown",
          "counter"->counter)
      )).toFuture
    }

    get("/hi/dare/:name") { req =>
      OK(s"""
        <html>
          <body><h1>Hi Dare ${req / "name"}</h1></body>
        </html>
      """).toFuture
    }
    
  }
  
  class MyUdderRouter(val documentRoot:String, val templateRoot:String) extends Router {

    import scala.pickling._
    import binary._
    import scala.pickling.binary.pickleFormat

    val someUser = UserData("Jimbino Jones", 55, 199.99, List(
      Pet("FooFee", 2),Pet("MooMoo",8),Pet("FooFee", 2),Pet("MooMoo",8),Pet("FooFee", 2),Pet("MooMoo",8),
      Pet("FooFee", 2),Pet("MooMoo",8),Pet("FooFee", 2),Pet("MooMoo",8),Pet("FooFee", 2),Pet("MooMoo",8),
      Pet("FooFee", 2),Pet("MooMoo",8),Pet("FooFee", 2),Pet("MooMoo",8),Pet("FooFee", 2),Pet("MooMoo",8),
      Pet("FooFee", 2),Pet("MooMoo",8),Pet("FooFee", 2),Pet("MooMoo",8),Pet("FooFee", 2),Pet("MooMoo",8),
      Pet("FooFee", 2),Pet("MooMoo",8),Pet("FooFee", 2),Pet("MooMoo",8),Pet("FooFee", 2),Pet("MooMoo",8),
      Pet("FooFee", 2),Pet("MooMoo",8),Pet("FooFee", 2),Pet("MooMoo",8),Pet("FooFee", 2),Pet("MooMoo",8)
    ))
    
    get("/my/udder/route/:name") { req =>
      OK(s"""
        <html>
          <body><h1>Hi Dare ${req / "name"}</h1></body>
        </html>
      """).toFuture
    }

    post("/a/user/as/json") { req =>
      val myUser = req.json[UserData]
      OK(s"you posted: ${myUser}").toFuture
    }

    get("/session/test") { req =>
      val previousValue = req.sess.getOrElse("sessionValue","boo ".b).s
      val newValue =  (previousValue + previousValue)
      req.sess("sessionValue") = newValue
      OK(s"previous value: $previousValue").toFuture
    }
    
    get("/pickle/and/session") { req =>
      println("entering pickle and session")
      val pickledValue = someUser.pickle.value
      req.sess("pickledObject") = pickledValue
      OK(s"object successfully pickled: $pickledValue").toFuture
    }
    
    get("/unpickle/the/session") { req =>
      val s = req.sess.getOrElse("pickledObject", null)
      val o = s.unpickle[UserData]
      println(s"unpickled session: $o")
      OK(s"object successfully unpickled: $o").toFuture
    }

  }
  
  new Scetty()
  
  	// routers
    .router(new MyRouter(docRoot,templateRoot))
    .router(new MyUdderRouter(docRoot,templateRoot))
    
    // https config
//    .ssl(true)
//    .keystore("c:/Users/jboone/Dropbox/cert/myKeystore")
////    .keystore("c:/dev/cert2/server.keystore")
//    .keypass("password")
    
    // server config
    .address("localhost")
//    .address("192.168.1.2")
    .port(80)
//    .address("192.168.1.2")
//    .port(443)

    .start
    

}