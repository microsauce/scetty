package org.microsauce.scetty

/**
 * Created by jboone on 3/15/14.
 */
trait BaseRouter {
  import UriUtils._
  import scala.concurrent._

  protected val uriHandlers = scala.collection.mutable.ListBuffer[HttpRequestHandler]()

  def get(uriPattern: String)(handler: Request => Future[Response]) {
    addHandler(GET, uriPattern, handler)
  }

  def getRoute(verb: HttpVerb, uri: String) = {
    uriHandlers.filter { handler => if (
        (verb == handler.verb || handler.verb.verb == "use") &&
          handler.uriPattern.regex.pattern.matcher(uri).matches
      ) true else false
    }
  }

  protected def addHandler(verb: HttpVerb, uriString: String, handler: Request => Future[Response]) {
    val uriPattern = parseUriString(uriString)
    uriHandlers += new HttpRequestHandler(verb, uriPattern, handler)
  }

}
