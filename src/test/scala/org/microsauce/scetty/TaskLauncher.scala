package org.microsauce.scetty

/**
 * Created by johboon on 7/23/2014.
 */

object TaskLauncher extends App {

  /* Define Tasks */
  val tasks = Map(
    "taill"       -> CygBash("tail /cydrive/c/servers/liferay-portal-6.0.6/tomcat-6.0.29/logs/catalina.log"),
    "tailj"       -> CygBash(""),
    "jboss"       -> Cmd(""),
    "jbossKill"   -> Cmd(""),
    "cyg"   	    -> Cmd("C:/opt/cygwin64/bin/mintty.exe -i /Cygwin-Terminal.ico -"),
    "fwd" 	      -> CygBash("/c/opt/scripts/port_forwarding/openAll"),
    "lr"          -> Cmd("c:\\servers\\liferay-portal-6.0.6\\tomcat-6.0.29\\bin\\catalina.bat run"),
    "lrKill"      -> Cmd("C:\\servers\\liferay-portal-6.0.6\\tomcat-6.0.29\\bin\\shutdown.bat"),
    "mysql51"     -> Cmd("C:\\servers\\mysql-5.1.73-winx64\\bin\\mysqld.exe"),
    "mysqlKill51" -> Cmd("C:\\servers\\mysql-5.1.73-winx64\\bin\\mysqladmin.exe -u root shutdown"),
    "mysql"       -> Cmd("C:\\servers\\mysql-5.6.19-winx64\\bin\\mysqld.exe"),
    "mysqlKill"   -> Cmd("C:\\servers\\mysql-5.6.19-winx64\\bin\\mysqladmin.exe -u root shutdown"),
    "fwdAll"      -> CygInteractiveBash("/cygdrive/c/opt/scripts/port_forwarding/openAll"),
    "fwdVcs"      -> CygInteractiveBash("/cygdrive/c/opt/scripts/port_forwarding/my_openVCSTunnels"),
    "killSsh"     -> CygBash("/cygdrive/c/opt/scripts/port_forwarding/closeAllTunnels")
  )

  new Scetty()
    .router(new TaskRouter(tasks))
    .port(8888)
    .start
}

class TaskRouter(val tasks:Map[String,Task]) extends DefaultRouter {

  import scala.concurrent._
  import scala.concurrent.ExecutionContext.Implicits.global

  /* Define web service end-point */
  get("/execute/:task") { req =>
    val taskName = req/"task"
    tasks.get(taskName) match {
      case None => ERR(s"Unknown task $taskName").toFuture
      case Some(task) =>
        future { task.go } // fire-and-forget
        OK(s"executed task: $taskName").toFuture
    }
  }
}

sealed trait Task {
  import scala.sys.process._

  val exec:String
  protected def buildCommand():String
  override def toString() = buildCommand()
  def go:Unit = toString().!
}

case class Cmd(exec:String) extends Task {
  def buildCommand() = s"cmd /c start $exec"
}
case class CygInteractiveBash(exec:String) extends Task {
  def buildCommand() = "cmd /c start c:/opt/cygwin64/bin/bash.exe -c \"PATH=/cygdrive/c/opt/cygwin64/bin ; "+exec+"\""
}
case class CygBash(exec:String) extends Task {
  def buildCommand() = "c:/opt/cygwin64/bin/bash.exe -c \"PATH=/cygdrive/c/opt/cygwin64/bin ; "+exec+"\""
}
case class MSysBash(exec:String) extends Task {
  def buildCommand() = s"C:\\opt\\Git\\bin\\sh.exe --login -c $exec"
}
case class Exec(exec:String) extends Task {
  def buildCommand() = exec
}

