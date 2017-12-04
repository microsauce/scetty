package org.microsauce.scetty

trait BaseRouter {

  import UriUtils._
  import scala.concurrent._

  protected val uriHandlers = scala.collection.mutable.ListBuffer[HttpRequestHandler]()

  def get(uriPattern: String)(handler: Request => Future[Response]) {
    addHandler(GET, uriPattern, handler)
  }

  def getRoute(verb: HttpVerb, uri: String) = {
    uriHandlers.filter { handler =>
      requestMatchesHandler(verb, uri, handler)
    }
  }

  def getUse(uri: String) = {
    uriHandlers.filter { handler =>
      requestMatchesHandler(USE, uri, handler)
    }
  }

  protected def addHandler(verb: HttpVerb, uriString: String, handler: Request => Future[Response]) {
    val uriPattern = parseUriString(uriString)
    uriHandlers += new HttpRequestHandler(verb, uriPattern, handler)
  }

  protected def requestMatchesHandler(verb: HttpVerb, uri: String, handler: Handler):Boolean = {
    (verb == handler.verb/* || handler.verb.name == USE.name*/) &&
      handler.uriPattern.regex.pattern.matcher(uri).matches
  }

}
