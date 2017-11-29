package org.microsauce.scetty

/**
 * Created by jboone on 6/11/2014.
 */
trait SimpleScettyApp extends App with DefaultRouter {

  val scettyContextBuilder:ScettyContextBuilder = ScettyContextBuilder.create
  scettyContextBuilder.addRouter(this)
//  val scetty = new Scetty()
//  scetty.router(this)

  def inetAddress(address:String) = {
    scettyContextBuilder.inetAddress(address)
    this
  }

  def port(port:Int) = {
    scettyContextBuilder.port(port)
    this
  }

  def router(router:Router) = {
    scettyContextBuilder.addRouter(router)
    this
  }

  def first(router:Router) = {
    scettyContextBuilder.prependRouter(router)
    this
  }

  def start:Unit = new Scetty(scettyContextBuilder.build).start
}
