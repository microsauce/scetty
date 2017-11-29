package org.microsauce.scetty.testutils

import java.io.IOException
import java.net.ServerSocket
import javax.rmi.PortableRemoteObject

import scala.util.control.Breaks._

object TestUtils {

  private val portRange = 49152 until 65535

  def reservePort:PortReservation = {
    new PortReservation(bindPortInRange(portRange))
  }

  private def bindPortInRange(portRange:Range):ServerSocket = {
    var result: ServerSocket = null

    breakable {
      for (portNumber <- portRange) {
        try {
          result = new ServerSocket(portNumber)
          break
        } catch {
          case e: IOException => println(s"Failed to bind on port [$portNumber] - [${e.getMessage}]")
        }
      }
    }

    result
  }

}
