package org.microsauce.scetty

import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}


/**
 * This class encapsulates a json object serializer.
 *
 * Created by jboone on 1/25/14.
 */

class Json(val obj:AnyRef) {
  implicit val formats = Serialization.formats(NoTypeHints)

  override def toString =  write(obj)

}
