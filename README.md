Scetty
======

Scetty is a simple library for quickly writing HTTP Netty servers in Scala. 

## Features:
* Sinatra-style URI based routing
* Convienient and elegant json serialization & deserialization
* Leverages Scala's composable Future
* Out-of-the-box middleware: Cookie Support, Session Support (client side)
* Template engine integration - Scalate by default


Example (hello scetty):
```scala
object HelloApp extends SimpleScettyApp {

  use("/hello/*") { req =>
    println("hello I'm middleware");
    req.next
  }

  get("/hello/:name") { req =>
    OK("Hello ${req/"name"}").toFuture
  }
  
  start
}
```

