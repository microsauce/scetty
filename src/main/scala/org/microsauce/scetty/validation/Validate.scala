package org.microsauce.scetty.validation

import java.util.ResourceBundle
import scala.collection.mutable
import java.util.Locale

object Validate {

}

/**
 * TODO move this package to a separate utils library
 * Created by jboone on 1/26/14.
 */
trait Validate {

  val bundleName:String
  val validators:List[Tuple3[String,()=>Boolean,String]]
//  val messages:Map[String,Map[String,String]]
//  val resourceBundle:ResourceBundle
//
//  ResourceBundle.getBundle("")
  private val bundles = new mutable.HashMap[String,ResourceBundle]()

  def errors:Option[List[Tuple2[String,String]]] = {
    val errs = validators.map { validator =>
      if ( validator._2() ) Some((validator._1,validator._3))
      else None
    }.flatten
    if (errs.isEmpty) None
    else Some(errs)
  }

  def errors(name:String) = {
    val errs = validators.map { validator =>
      if ( name == validator._1 && validator._2() ) Some((validator._1,validator._3))
      else None
    }.flatten
    if (errs.isEmpty) None
    else Some(errs)
  }


// TODO
  // string of the form
  private def getMessage(key:String,locale:String) {
    // TODO parse out the parameter values
    // - some.message.id::Jimmy::35
    // - Error {0} is a deuche, he has been for {1} years.::Jimmy::35
//    if ( resourceBundle != null ) {
//      resourceBundle.getString(key)
//    }

  }

  private def getBundle(locale:String) = {
//    if ( !bundles.contains(locale) ) {
//      val b = ResourceBundle.getBundle(bundleName,Locale.forLanguageTag(locale))
//      bundles(locale) = b
//      b
//    } else {
//      bundles(locale)
//    }
  }

}
