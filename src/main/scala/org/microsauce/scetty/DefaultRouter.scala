package org.microsauce.scetty

/**
 * A Router sub-trait that uses default documentRoot and templateRoot values.
 */
trait DefaultRouter extends Router {
  
  /**
   * The router document root (static resources) - [working directory]/docs
   */
  val documentRoot = Router.docRoot
  
  /**
   * The router scalate template root - [working directory]/templates
   */
  val templateRoot = Router.templateRoot
}
