package org.microsauce.scetty.testutils

import java.net.ServerSocket

class PortReservation(val serverSocket: ServerSocket) {

  private def portNumber:Int = serverSocket.getLocalPort()

  def releasePort:Int = {
    val result = serverSocket.getLocalPort()
    serverSocket.close()
    result
  }

}
