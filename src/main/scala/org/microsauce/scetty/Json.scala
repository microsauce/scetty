package org.microsauce.scetty

import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}


/**
 *
 * Json()
 *
 * Created by jboone on 1/25/14.
 */

class Json(val obj:AnyRef) {
  implicit val formats = Serialization.formats(NoTypeHints)
  override def toString = {
  //  JacksMapper.writeValueAsString(obj)
    write(obj)
  }
}
