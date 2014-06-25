Scetty
======

A simple library for writing Netty HTTP servers in Scala.

## Introduction

The Scetty API was inspired by Twitter's Finatra, Express.js, and a tip-of-the-hat to the Play framework too.

## Features:
* Build fully asynchronous servers using Netty and Scala Futures
* Dynamic-style server DSL for Scala
* Express.js/Sinatra style routing
* Convenient and elegant json support
* Pre-defined middleware: Cookie Support, Session Support
* Scalate template integration
* SSL Support

## Glossary of Terms

**Handler**: a function of type Request=>Future\[Response\] dispatched to service an HTTP request

**Middleware**: a handler that performs an intermediate task, defined via the 'use' method

**Route**: an ordered sequence of handlers assembled to service an HTTP request

**End-Point**: the ultimate handler in the route, defined via 'get','post','put', or 'delete'

# Getting Started

For starters you must define one or more routers and register them with a Scetty server instance.  As follows:

```scala
class HelloWorld extends DefaultRouter {
  // hello handler
  get("/hello/:name") { 
    OK(s"Hello ${req/"name"}").toFuture
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

If no matching end-point is defined Scetty will presume the request URI represents a static resource (a file) and
attempt to read it from the file system.

## Error Handler

If any of your request handlers fail to handle their own exceptions control is passed to the default error handler.  The default 
error handler is defined the same way as any other GET handler, but it must have a uri pattern of "/error":

```scala
  get("/error") { req =>
    ERR(req.error.stackTrace,"text/plain").toFuture
  }
```

## More on Middleware

Middleware can be used to parse and/or decorate the request or it can be used to handle cross-cutting concerns like logging,
authentication, data loading, caching, etc.  Middleware is defined with the "use" method:

```scala
class Restricted extends DefaultRouter {
  // check password middleware
  use("/secret/*") { req =>
    req?"password"|"WRONG" match {
      case "super secret" => req.next
      case _ => FORBIDDEN("Access Denied!!!").toFuture
    }
  }
  
  get("/secret/stuff") { req =>
    OK("Shh! Don't tell anybody!").toFuture
  }
  
}
```

Any request to a uri of the form "/secret/*" will first be dispatched to the "check password" middleware.  When the correct
password is given the next handler in the route (req.next) is called, otherwise an error is given. 

## URI Patterns

All handlers are bound to a URI pattern.  URI patterns may contain parameter names and/or wildcards or they may be
static.  For example:

```scala
  // URI pattern with two parameters (brand & size).  Matches: "GET /shoe/nike/11" and "GET /shoe/adidas/12" for example
  get("/shoe/:brand/:size") { req => ... } 

  // URI pattern with a wildcard. Matches: "GET /dog/fido" and "GET /dog/rex" for example
  get("/dog/*") { req => ... }
    
  // URI pattern with a wildcard. Matches: "GET /cat/fish/Mittens/tweetybirds" for example
  get("/cat/*/:name/*") { req => ... }  

  // Static URI. Matches ONLY "GET /me/a/cup/of/coffee"  
  get("/me/a/cup/of/coffee") { req => ... } 
```

### Parameters

Parameters are easily extracted from the request uri, for example:

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
    val firstWildcard = req/"*_0"  // get the wildcard value at index 0 with parameter name "*_0"
    val secondWildcard = req/"*_1" // get the wildcard value at index 1 with parameter name "*_1"
    ...
  }
```

## Response Methods

The Router trait has four overloaded Response methods: OK (200), ERR (500), NOT_FOUND (404), and FORBIDDEN (403).  

```scala
  import org.microsauce.scetty.Router._

  get("/divide/:dividend/:divisor") { req =>
    try {
      val dividend = (req"dividend").toFloat   
      val divisor = (req"divisor").toFloat  
      OK(s"Quotient: ${dividend/divisor}","text/plain").toFuture
    } catch {
      case e: NumberFormatException => ERR(s"Bad input ${req/"dividend"} - ${req/"divisor"}").toFuture
      case e: ArithmeticException => ERR(s"Check your numbers: ${e.getMessage}", "text/plain").toFuture
      case e: Throwable => ERR(new File(documentRoot+"/unknown_error.txt")).toFuture 
        // FYI - documentRoot requires import org.microsauce.scetty.Router._
    }
  }
```

By default all strings passed to these response methods have content type of "text/html", all binary data is sent as 
"application/octet-stream", and json objects as "application/json".  When sending a file Scetty chooses a content type 
based on the file extension (via MimeTable).

Alternatively you may instantiate your own response object as follows:

```scala
  get("/my/response") { req =>
    // Response:(HttpResponseStatus/*status*/,Any/*source*/,String/*contentType*/)
    new Response(HttpResponseStatus.200,"My Response","text/plain").toFuture
  }
```

The second parameter to the Response constructor is the content source object.  This value may be a String, File, ByteBuf, or
Array\[Byte\].

# Request

The Scetty Request class wraps a Netty FullHttpRequest object and exposes all of its methods 
(see [FullHttpRequest](http://netty.io/4.0/api/io/netty/handler/codec/http/FullHttpRequest.html)).  It also provides a 
wealth of additional functionality.

## Additional Methods

**def json\[T\]:T** - de-serialize the (Json) request body and return an object of the given type
 
**def bodyString:String** - return the request body content as a String

**def contentType:String** - return the request content type

**def method:String** - return the request method

**def uri:String** - return the request uri (excluding the query string)

**def apply\[T\](attrName:String):T** - retrieve an attribute from the request as an instance of the given type
```scala
  val myData = req[MyData]("myData")
```  
 
**def update(attrName:String,value:Any)** - set the value of a request attribute
```scala
  req("myData") = myData
```

**def next:Future\[Response\]** - execute the next handler in the route

## Additional Values/Variables

**cookies : Map\[String,String\]** - the request cookie map - available only when cookieSupport is in use 

**sess : Map\[String,Array\[Byte\]\]** - the session attribute map (Note: the session API is not yet final) 

**error : Throwable** - this variable contains an exception thrown from a handler 

## Operators

**def / : (paramName:String):String** - retrieve a URI parameter from the request 

**def &amp; (paramName:String):Option\[String\]** - retrieve a form parameter from the request 

**def ? (paramName:String):Option\[String\]** - retrieve a query string parameter from the request
 
**def ^^ (paramName:String):Option\[ByteBuf\]** - retrieve multi-part form data as a ByteBuf (a file upload, for example) 

## Monkey Patches

To provide a terse programming DSL an implicit class is defined to augment Option with a pipe operator:

```scala
import org.microsauce.scetty.Router._

. . .

get("/*") { req =>
  val name = req?"name"|"Jimbo"  // equivalent to req.?("name").getOrElse("Jimbo")
  . . .
}

```

"|" in this context is equivalent to "getOrElse".

## Session

TODO the session api is not yet finalized

# Futures

You may recall that handlers must have a return type of Future\[Response\].  Thus far we have used the .toFuture method
to create and return a completed Future.  If, however, your handler uses the 'future' construct (example 1) or makes use 
of libraries that return Futures (example 2) you can do the following:

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
      yield OK(render("/data/template.jade", Map("data"->data))) // yield a response from within a Future 
  }
```

# Json

Json data is easily de-serialized from the request:

```scala
  case class MyData(name:String, age:Int)

  . . .

  post("/my/data") { req =>
    val myData = req.json\[MyData\]
    . . . 
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
import io.netty.handler.codec.http.DefaultCookie
. . .
class CookieEnablingRouter extends DefaultRouter {
  use(cookieSupport)
  . . .
  
  get("/") { req =>
    // retrieve a cookie from the request
    val myCookie = req.cookies("myCookie")
    
    . . .
    
    // send a cookie in the response
    OK("let's send a cookie")
      .cookie(new DefaultCookie("key","value")) 
      .toFuture
  }
}
```

When using the cookieSupport middleware cookies are decoded from the request header and added to the request cookies
map and encoded and sent in the response, they are accessible by name.  

# Router trait

Thus far we have extended DefaultRouter in our router definitions.  DefaultRouter provides sensible defaults for two 
Router values: documentRoot:String and templateRoot:String.

**documentRoot**: determines the root folder from which this Router will load static resources (files).

**templateRoot**: determines the root folder from which this Router will load templates.

If these default values are insufficient you may extend the Router trait directly and initialize them yourself:
  
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

# More to Come . . .