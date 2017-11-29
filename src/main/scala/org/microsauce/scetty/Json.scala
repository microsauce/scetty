package org.microsauce.scetty

import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}

/**
 * Encapsulate a json object serializer.
 */

class Json(val obj:AnyRef) {
  implicit val formats = Serialization.formats(NoTypeHints)

  override def toString =  write(obj)
}
