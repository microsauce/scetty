package org.microsauce.scetty

/**
 * Created by jboone on 1/27/14.
 */

case class Cat(name:String,lives:Int)

object Sandbox extends App {

  class Request(hasValue:Boolean) {
    def /(key:String):Option[String] = {
      if ( hasValue ) Some("moo")
      else None
    }
    def %(key:String):Option[String] = {
      if ( hasValue ) Some("moo")
      else None
    }
    def ##(key:String):Option[String] = {
      if ( hasValue ) Some("moo")
      else None
    }
    def ^(key:String):Option[String] = {
      if ( hasValue ) Some("moo")
      else None
    }
    def apply[T](attrKey:String):Option[T] = {
      if (hasValue) Some(Cat("Steve", 8).asInstanceOf[T])
      else None
    }
  }

  val reqWithValue = new Request(true)
  val reqWithOutValue = new Request(false)

  println("\nuri parameters:")
  println(s"\twit value: ${(reqWithValue/"hey").getOrElse("NOTmoo")}")
  println(s"\twit OUT value: ${(reqWithOutValue/"hey").getOrElse("NOTmoo")}")

for (
  hey <- reqWithValue/"hey";
  yo <- reqWithValue/"yo"
) {
  println(s"println $hey - $yo")
}
  for (
    hey <- reqWithValue/"hey";
    yo <- reqWithOutValue/"yo"
  ) {
    println(s"println $hey - $yo")
  }

  println("\nreq attr:")
  println(s"\twit value: ${reqWithValue[Cat]("hey").getOrElse(Cat("Default Cat", 9))}")
  println(s"\twit OUT value: ${reqWithOutValue[Cat]("hey").getOrElse(Cat("Default Cat", 9))}")

  println("\nquery parameters:")
  println(s"\twit value: ${(reqWithValue%"hey").getOrElse("NOTMoo")}")
  println(s"\twit OUT value: ${(reqWithOutValue%"hey").getOrElse("NOTMoo")}")

  println("\nform parameters:")
  println(s"\twit value: ${(reqWithValue##"hey").getOrElse("NOTMoo")}")
  println(s"\twit OUT value: ${(reqWithOutValue##"hey").getOrElse("NOTMoo")}")

  /*
  (req/"hey").getOrElse("NOTmoo")
  req/"hey"
   */
  val name = "Steve"
  val fun = List(
    {name=="Steve"},
    {name.length>3}
  )

  println(s"fun class: ${fun.getClass}::")
  fun.foreach {f=>
    println(f)
  }
}
