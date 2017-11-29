package org.microsauce.scetty.util

import org.scalatest._
import org.junit.runner.RunWith
import org.scalatest.junit._

@RunWith(classOf[JUnitRunner])
class ScalaUtilsSpec extends FlatSpec with Matchers {

  "coalesce" should "return the default value" in {
    val actual = ScettyUtil.coalesce("stuff", null, null)

    actual should be ("stuff")
  }

  "coalesce" should "return the third value" in {
    val actual = ScettyUtil.coalesce("stuff", null, null, "third", "forth")

    actual should be ("third")
  }

  "coalesce" should "return the second value" in {
    val actual = ScettyUtil.coalesce(99, null, 7, 8, 9)

    actual should be (7)
  }

//  "coalesce" should "return the default value" in {
//    val actual = ScettyUtil.coalesce(99, _)
//
//    actual should be (7)
//  }
}