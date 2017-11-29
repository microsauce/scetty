package org.microsauce.scetty

import scala.util.matching.Regex
import scala.util.matching.Regex.Match

/**
 * This class encapsulates a regular expression (derived from the handler uri pattern) and the
 * list of embedded identifiers
 */
case class UriPattern(uriPatternString:String, regex: Regex, params: List[String])

/**
 * This object defines functions for handling request uri's and uri patterns.
 */
object UriUtils {

  import scala.util.matching.Regex

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
    val values = (
      for (
        thisMatch <- uriPattern.regex.findAllMatchIn(uri);
        ndx <- 1 until (thisMatch.groupCount + 1);
        value = thisMatch.group(ndx)
      ) yield value
    ).toList

    (keys zip values) toMap
  }

  /**
    * Parse named and wildcard identifiers from URI pattern string
    *
    * @param uriPattern
    * @return
    */
  private def identifiersInOrder(uriPattern: String) = {
    val tokens = for (
      thisMatch <- uriParmPattern.findAllMatchIn(uriPattern);
      token = getToken(thisMatch)
    ) yield token

    var ndx = -1
    tokens.toList.map { token =>
      ndx += 1
      if (token == "*") s"*_$ndx" else token
    }
  }

  private def getToken(matcher:Match):String =
    if (matcher.groupCount > 1 && matcher.group(2) != null)
      matcher.group(2)
    else "*"

  private def runtimePattern(uriExpression: String, currentPattern: Regex, replacementPattern: String) = {
    currentPattern.replaceAllIn(uriExpression, m =>
      replacementPattern).r
  }
}