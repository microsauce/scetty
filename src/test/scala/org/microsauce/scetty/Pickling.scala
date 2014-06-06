package org.microsauce.scetty

case class Robber(name:String,age:Int)
case class Cop(badgeNumber:Int,rank:String,pinched:List[Robber])

object Pickling extends App {

  import scala.pickling._
  import binary._
  import org.microsauce.scetty.coder.Coder._

  val cop = Cop(1234,"Sqt",List(Robber("Steve",39),Robber("Foo",25)))

  val copPickle = cop.pickle
  val copPickleValue = copPickle.value
  println(s"copPickle Len: ${copPickleValue.length}")
  val base64Encoded = base64Coder.encode(copPickleValue)
  println(s"base64 encoded pickle String    Len: ${base64Encoded.length}")
  println(s"base64 encoded pickle ArrayByte Len: ${base64Encoded.getBytes.length}")

}
