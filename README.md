Scetty
======

Scetty is a simple library for writing Netty HTTP servers in Scala. 

## Features:
* A dynamic-style server DSL for Scala
* Express.js/Sinatra style routing
* Convenient and elegant json support
* Pre-defined middleware: Cookie Support, Session Support (client side)
* Template engine integration - Scalate by default
* Built on Netty
* A feature rich Request class adding support for cookies, sessions, uri parameters, attributes, json deserialization, and more ...


CAUTION! THIS README IS A WORK IN PROGRESS
==========================================

Example (hello scetty):
```scala

object HelloApp extends SimpleScettyApp {

  use("/hello/*") { req =>
    println("hello I'm middleware")
    req.next
  }

  get("/hello/:name") { req =>
    OK("Hello ${req/"name"}").toFuture
  }
  
  start
}
```

## Handlers

A handler is a function defined to service an HTTP request.  Handlers are of the type Request=>Future[Response].  There are two
types of handlers:  middleware and end-points.  More on that below.

## Routing

In Scetty a route is a sequence of handlers assembled at runtime to service a given request method and uri.

### Router trait

Routes are defined using one or more routers.  To define a router extend the Router trait and call http verb methods in
the class body to register your handlers with the Scetty runtime.  For example:

```scala
class MyRouter extends Router {

  // logging middleware
  use { req =>
    println(s"serving ${req.method}: ${req.uri}")
    val futureResponse = req.next
    println(s"\t${req.method}: ${req.uri} has been served")
    futureResponse
  }

  get("/my/data") { req =>
    val myData = dataService.getMyData()
    OK(json(myData)).toFuture
  }

  get("/view/other/data") { req =>
    val myOtherData = dataService.getOtherMyData()
    OK(render("/view/mydata.jade", myOtherData)).toFuture
  }

}
```

This router defines handlers for retrieving my data and other data.  It also defines middleware (via the "use" method) for
logging request information to the console.  The single argument version of the method is applied to all request uri's, it
is equivalent to: use("/*") { req => . . . }

#### Http Verb Methods

The Router trait has five different methods for registering HTTP handlers with the the Scetty runtime.  The four HTTP verb methods
'get', 'post', 'put', and 'delete' and the middleware method 'use'.

As the names imply, the first four respond to the HTTP methods of the same name.  'use' is for defining middleware.

### URI Patterns

These are simple patterns used to define the request uri's which apply to a given handler.  These patterns may be static
or they may contain any number of embedded parameter names (or the form :myParameterName) or wildcards (*).  For example,
to define a middleware handler to apply to all order uris:

```scala
  use("/order/*") { req =>
    . . .
  }
```

Or to define an end-point with embedded parameter 'name':

```scala
  get("/hello/:name") { req =>
    OK(s"Hello ${req/"name"}!").toFuture
  }

```

### Middleware

The purpose of middleware in Scetty is to handle cross-cutting concerns, such as authentication, data caching, or logging for
example.

### Response Methods (OK,ERR,NOT_FOUND)

All Scetty handlers must return a Future[Response].  The Router trait provides several convenient methods for creating response
objects.

OK

ERR

NOT_FOUND

### Other Methods (json, render)

#### json(obj) - AnyRef=>Json

Encapsulate the parameter in a Json object.  This object may be passed to any of the response methods; it will be serialized
as JSON in the HTTP response with a content type of "application/json".

```scala
  get("/dogs") { req =>
    val allMyDogs = dogService.getMyDogs()
    OK(json(allMyDogs)).toFuture
  }
```

#### render(templatePath,model) - (String,Map[String,Any])=>String

Every Router instance has a "render" variable.  By default this function uses Scalate to render views, however it is a \
public variable and can be reset to provide support for other template engines if desired.

```scala
  get("/view/data") { req =>
    val model = dataService.getMyData()
    OK(render("/view/data.jade", model)).toFuture
  }
```

## Scetty class

The Scetty class is an abstraction encapsulating the Netty runtime.  It provides a default channel pipeline initializer
implementation, which you can substitute for your own, and many other convenience methods to custom configure your server.

## Request (see netty JavaDoc)


## More to Come . . .

For more examples view the src/test/scala folder.

