Scetty
======

A simple library for writing Netty HTTP servers in Scala.

## Features:
* Fully asynchronous IO using Netty and Scala Futures
* Dynamic-style server DSL for Scala
* Express.js/Sinatra style routing
* Convenient and elegant json support
* Pre-defined middleware: Cookie Support, Session Support (client side)
* Template engine integration - Scalate by default
* SSL Support
* Built on Netty
* Request and session attributes, uri, query string, and form parameter maps


# Getting Started

For starters you must define one or more routers and register them with a Scetty instance.  As follows:

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

# Routes

In Scetty a route is a sequence of handlers assembled at runtime to service a given request method and uri.
When Scetty receives a request it scans all registered routers, adds all matching middleware (more on that
below) and finally appends the first matching HTTP verb handler (get, post, put, or delete) to terminate the sequence:

```scala
// TODO example here
``` 

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

Middleware are all the functions added to your route sequence ahead of the selected, matching end-point.  Middleware 
can be used to parse and/or decorate the request or it can be used to handle cross-cutting concerns like logging, 
authentication, data loading, caching, etc.

```scala
// TODO
```

## URI Patterns

### Parameters

### Wildcards

# Request

## Operators (/ & ? ^^) and apply

## Session

# Futures

# json

# Templates

# Cookies

# SimpleScettyApp
