package org.microsauce.scetty

/**
 * Created by jboone on 6/11/2014.
 */
trait SimpleScettyApp extends App with DefaultRouter {

  val scetty = new Scetty()
  scetty.router(this)

  def address(address:String) = {
    scetty.address(address)
    this
  }

  def port(port:Int) = {
    scetty.port(port)
    this
  }

  def router(router:Router) = {
    scetty.router(router)
    this
  }

  def first(router:Router) = {
    scetty.first(router)
    this
  }

  def start = scetty.start
}
