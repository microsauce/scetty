package org.microsauce.scetty

import scala.util.matching.Regex

/**
 * This encapsulates a regular expression (derived from the handler uri pattern) and the
 * list of associated embedded uri identifiers
 */
case class UriPattern(val uriPatternString:String, val regex: Regex, val params: List[String])

/**
 * This object defines functions for handling request uri's and uri patterns.
 */
object UriUtils {

  import scala.util.matching.Regex
  import scala.collection.breakOut

  private val uriParmPattern = """(:([a-zA-Z0-9]+))|\*""".r
  private val replacementPattern = "(.+)"

  /**
   * Parse the uriPattern string to produce a UriPattern object
   * @param uriPattern
   * @return
   */
  def parseUriString(uriPattern: String): UriPattern = {
    val params = identifiersInOrder(uriPattern)
    val rtPattern = runtimePattern(uriPattern, uriParmPattern, replacementPattern)
    new UriPattern(uriPattern, rtPattern, params)
  }

  /**
   * Extract parameter values from the request uri.
   * @param uri
   * @param uriPattern
   * @return
   */
  def extractValues(uri: String, uriPattern: UriPattern) = {
    val keys = uriPattern.params
    val values = (for (
      thisMatch <- uriPattern.regex.findAllMatchIn(uri);
      ndx <- 1 until (thisMatch.groupCount + 1);
      value = thisMatch.group(ndx)
    ) yield value).toList
    (keys zip values) toMap
  }

  private def identifiersInOrder(uriPattern: String) = {
    val tokens = for (
      v <- uriParmPattern.findAllMatchIn(uriPattern);
      token = if (v.groupCount > 1 && v.group(2) != null) v.group(2).toString else "*"
    ) yield token
    var ndx = -1
    tokens.toList.map { t =>
      ndx += 1
      if (t == "*") s"*_$ndx" else t
    }
  }

  private def runtimePattern(uriExpression: String, currentPattern: Regex, replacementPattern: String) = {
    currentPattern.replaceAllIn(uriExpression, m =>
      replacementPattern).r
  }
}