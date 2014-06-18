Scetty
======

A simple library for writing Netty HTTP servers in Scala.

## Introduction

The Scetty API was inspired by Twitter's Finatra, Express.js, with a tip-of-the-hat to the Play framework too.

## Features:
* Build fully asynchronous servers using Netty and Scala Futures
* Dynamic-style server DSL for Scala
* Express.js/Sinatra style routing
* Convenient and elegant json support
* Pre-defined middleware: Cookie Support, Session Support (client side)
* Template engine integration - Scalate by default
* SSL Support
* Built on Netty
* Uri, query string, and form parameter maps

## Glossary of Terms

**Handler**: a function of type Request=>Future[Response] dispatched to service an HTTP request

**Middleware**: a handler that performs an intermediate task, defined via the 'use' method

**Route**: an ordered sequence of handlers assembled to service an HTTP request

**End-Point**: the ultimate handler in the route, defined via 'get','post','put', or 'delete'

# Getting Started

For starters you must define one or more routers and register them with a Scetty server instance.  As follows:

```scala
class HelloWorld extends DefaultRouter {
  // hello handler
  get("/hello/:name") { 
    OK("Hello ${req/"name"}").toFuture
  }
}

new Scetty()
  .router(new HelloWorld())
  .start
```

You can register multiple routers as follows:

```scala
new Scetty()
  .router(new MyRouter())
  .router(new MyOtherRouter())
  .start
```

The 'hello' handler in the first example will respond to GET requests to uri's of the form '/hello/*'.  

# Routers

When Scetty receives a request it scans all registered routers, adds all matching middleware (more on that
below) and finally appends the first matching end-point (defined via get, post, put, or delete):

```scala
class GradeRouter extends DefaultRouter {

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

  // get grade end-point
  get("/grade/:grade") { req =>
    OK(s"The grade ${req/"grade"} has earned you a ${req("reward")}").toFuture
  }

}
```

In this example, to service a request to "GET /grade/A" Scetty will create a route beginning with "reward middleware"
and ending with "get grade end-point" yielding a response of "The grade A has earned you a gold star".

If no matching verb handler is defined Scetty will presume the request URI represents a static resource (a file) and
attempt to read it from the file system.

## Error Handler

If any of your request handlers fail to handle their own exception control is passed to the default error handler.  The default 
error handler is defined the same way as any other hander, but it must have a uri pattern of "/error":

```scala
  get("/error") { req =>
    ERR(req.error.stackTrace,"text/plain").toFuture
  }
```

## More on Middleware

Middleware can be used to parse and/or decorate the request or it can be used to handle cross-cutting concerns like logging,
authentication, data loading, caching, etc.  Middleware is defined using the "use" method:

```scala
// TODO
```

## URI Patterns

All handlers are bound to a URI pattern.  URI patterns may contain parameter names and/or wildcards or they may be
static.  For example:

```scala
  get("/shoe/:brand/:size") { req => ... } // URI pattern with two parameters.  Matches: "GET /shoe/nike/11" and "GET /shoe/adidas/12"

  get("/dog/*") { req => ... }  // URI pattern with a wildcard. Matches: "GET /dog/fido" and "GET /dog/rex"

  get("/me/a/cup/of/coffee") { req => ... } // Static URI. Matchs ONLY "GET /me/a/cup/of/coffee"
```

### Parameters

Parameters can be extracted from request URI's, for example:

```scala
  get("/auto/:make/:model") { req =>
    val make = req/"make"
    val model = req/"model"
    ...
  }
```

In this example two URI parameters are defined in the URI pattern ("make" and "model").  These values can be retrieved from the
request object via the "/" operator.

### Wildcards

Wildcard values may also be extracted from the request:

```scala
  get("/fish/*/quantity/*") { req =>
    // wildcard indices are zero-based
    val firstWildcard = req/"*_0"  // get the wildcard at index 0 with parameter name "*_0"
    val secondWildcard = req/"*_1"
    ...
  }
```


# Request

The Scetty Request wraps a Netty FullHttpRequest object and exposes all of its methods 
(see [FullHttpRequest](http://netty.io/4.0/api/io/netty/handler/codec/http/FullHttpRequest.html)).  It also provides a 
wealth of additional functionality.

## Additional Methods

def json\[T\]:T - de-serialize the (Json) request body and return an object of the given type
 
def bodyString:String - return the request body content as a String

def contentType:String - return the request content type

def method:String - return the request method

def uri:String - return the request uri (excluding the query string)

def apply\[T\](attrName:String):T - retrieve an attribute from the request as an instance of the given type
```scala
  req[MyData]("myData")
```  
 
def update(attrName:String,value:Any) - set the value of a request attribute
```scala
  req("myData") = myData
```

def next:Future\[Response\] - execute the next handler in the route

## Additional Values/Variables

cookies:Map\[String,String\] - the request cookie map - available only when cookieSupport is in use 

sess:Map\[String,Array\[Byte\]\] - the session attribute map (Note: the session API is not yet final) 

error:Throwable - this variable contains an exception thrown from a handler 

## Operators (/ & ? ^^) and apply

def / (paramName:String):String - retrieve a URI parameter from the request 

def &amp; (paramName:String):Option\[String\] - retrieve a form parameter from the request 

def ? (paramName:String):Option\[String\] - retrieve a query string parameter from the request
 
def ^^ (paramName:String):Option\[ByteBuf\] - retrieve multipart form data as a ByteBuf (for a file update, for example) 

## Monkey Patches

To provide a terse programming DSL an implicit class is defined to augment Option:

```scala
import org.microsauce.scetty.Router._

. . .

get("/*") { req =>
  val name = req?"name"|"Jimbo"  // equivalent to req.?("name").getOrElse("Jimbo")
  . . .
}

```

The pipe operator in this context is equivalent to the method "getOrElse".

## Session

TODO the session api is not yet finalized

# Futures

You may recall that handlers must have a return type of Future[Response].  Thus far we have used the .toFuture method
to create and return a completed Future.  If, however, your handler uses the 'future' construct or makes use of libraries
that return Futures you can do the following:

```scala
  // example 1: future construct
  get("/compute/:param1/:param2") { req => future {
      val data = someLongRunningComputation(req/"param1",req/"param2")
      OK(json(data))  // calling OK from within a future
    }
  }
  
  // example 2:  
  get("/data") { req =>
    val futureData:Future[Data] = getFutureData() 
    for ( data <- futureData ) 
      yield OK(render("/data/template.jade", Map("data"->data))) // this for comprehension yields a Future[Response] 
  }
```

# Json

Json data is easily de-serialized from the request:

```scala
  case class MyData(name:String, age:Int)

  . . .

  post("/my/data") { req =>
    val myData = req.json[MyData] 
  }
```

To serialize an object as Json in the response:

```scala
  get("/my/data") { req =>
     . . .
    OK(json(mydata)).toFuture
  }
```

# Templates

To render a template:

```scala
  get("/view/data") { req =>
    . . .
    // render: (String,Map[String,Any]=>String
    OK(render("/view/data.jade", Map("data"->data))).toFuture
  }
```

# Cookies

Cookie support can be added to a Scetty application by adding cookieSupport middleware to a router: 

```scala
import org.microsauce.scetty.Router._
. . .
class CookieEnablingRouter extends DefaultRouter {
  use(cookieSupport)
  . . .
  
  get("/") { req =>
    val cookieData = req.cookies("myCookie")
    . . .
  }
}
```

# Router trait

Thus far we have extended DefaultRouter in our router definitions.  DefaultRouter provides sensible defaults for two 
Router values: documentRoot:String and templateRoot:String.

documentRoot: determines the root folder from which this Router will load static resources (files).

templateRoot: determines the root folder from which this Router will load templates.

If these default values are insufficient you may extend the Router trait directly and initialize and initialize
them yourself:
  
```scala
class MyRouter(val documentRoot:String,val templateRoot:String) extends Router {
  . . .
}

val myRouterInstance = new MyRouter("/my/document/root","/my/template/root")
```

# SimpleScettyApp

To simplify the creation of Scetty servers you can alternatively extend the SimpleScettyApp trait: 

```scala
object HelloWorld extends SimpleScettyApp {

  get("/hello/:name") { req =>
    OK(s"hello there ${req/"name"}").toFuture
  }
  
  start
}
```
