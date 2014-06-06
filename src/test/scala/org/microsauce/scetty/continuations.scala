
package org.microsauce.scetty

object continuations extends App {

  import scala.util.continuations._
  import scala.collection.mutable
  import java.util.UUID

  def go = reset {
    println("Welcome!")
    val first = ask("Please give me a number")
    val second = ask("Please enter another number")
    printf("The sum of your numbers is: %d\n", first + second)
  }

  val sessions = new mutable.HashMap[UUID, Int=>Unit]
  def ask(prompt: String): Int @cps[Unit] = shift {
    k: (Int => Unit) => {
      val id = UUID.randomUUID()
      printf("%s\nrespond with: submit(0x%x, ...)\n", prompt, id)
      sessions += id -> k
    }
  }

  go

}
