package org.microsauce.scetty

import scala.concurrent._

// TODO
/*
rather than have a separate handler for Websockets do the following:
- val route = findRoute(req.uri)
- if ( route.endPoint.isWS ) new WebSocket(route, wsHandshake())
- else  execute the route
 */


/*
 TODO a new instance of the handler will be created for every new connection.  The router(s) will
 be a property of the handler (maybe).
 */
class WebSocketRouter extends BaseRouter {

  import UriUtils._

  //  protected val handlers = scala.collection.mutable.ListBuffer[WSHandler]()

//  protected override val uriHandlers = scala.collection.mutable.ListBuffer[Handler]() // WS
  private val handlers = scala.collection.mutable.ListBuffer[WebSocketHandler]() // use

  def use(handler:Any=>Future[Any]):Unit = use("/*")(handler)

  def use(uriString:String)(handler:Any=>Future[Any]) {
    addWSHandler(uriString,handler,false)
  }

  // TODO 'send' to replace ws
  // TODO 'push' broadcast to all channels
  // TODO acknowledgment - fire and forget
  def on(uriString:String)(handler:Any=>Future[Any]):Unit = addWSHandler(uriString,handler,true)

  def send(uriString:String)(handler:Any=>Future[Any]) {

  }

  // appropriate for fire-and-forget data streaming
  def push(uriString:String)(handler:Any=>Future[Any]) {
    push(uriString,{_=>true})(handler)
  }

  def push(uriString:String,guard:Request=>Boolean)(handler:Any=>Future[Any]) {
    // TODO
  }

  // appropriate for fire-and-forget data streaming
  def pull(uriString:String)(handler:Any=>Future[Any]) {
    // TODO
  }

  private def addWSHandler(uriString:String,handler:Any=>Future[Any],endPoint:Boolean) {
    val uriPattern = parseUriString(uriString)
    handlers += new WebSocketHandler(GET, uriPattern, handler, endPoint)
  }

  def getWSRoute(uri:String) = {
    handlers.filter { handler => if (
        handler.uriPattern.regex.pattern.matcher(uri).matches
      ) true else false
    }
  }

  // TODO route is established during the handshake
//  def getRoute(uri: String) = {
//    handlers.filter { handler => if (
//        handler.uriPattern.regex.pattern.matcher(uri).matches
//      ) true else false
//    }
//  }
}

//class WSHandler(
//  val uriPattern: UriPattern,
//  val callBack: Request => Future[Response]
//) {
//  override def toString = {
//    s"[ws: $uriPattern]"
//  }
//
//}
