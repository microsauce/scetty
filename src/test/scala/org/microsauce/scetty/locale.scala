package org.microsauce.scetty

import java.util.Locale

/**
 * Created by jboone on 2/11/14.
 */
object locale extends App {
  val loc = Locale.forLanguageTag("en-US")

  println(s"loc: $loc")
  println(s"country : ${loc.getCountry}")
  println(s"language: ${loc.getLanguage}")

}
