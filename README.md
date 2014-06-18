Scetty
======

A simple library for writing Netty HTTP servers in Scala.

## Introduction

The Scetty API was inspired by Twitter's Finatra, Express.js, with a tip-of-the-hat to the Play framework too.

## Glossary of Terms

Handler: a function of type Request=>Future[Response] dispatched to service an HTTP request
Middleware: a handler that performs an intermediate task
Route: an ordered sequence of handlers assembled to service an HTTP request
End-Point: the ultimate handler in the route

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

# Monkey Patches (move to Request section - or make note of it there also)

To provide a terse and (somewhat) intuitive programming DSL several implicit classes are defined in the Router companion object
and are made available with the following import:

```scala
import org.microsauce.scetty.Router._
// TODO
```

# Request

The Scetty request class wraps a Netty FullHttpRequest object.

## Operators (/ & ? ^^) and apply

## Session

# Futures

# json

# Templates

# Cookies

# SimpleScettyApp
