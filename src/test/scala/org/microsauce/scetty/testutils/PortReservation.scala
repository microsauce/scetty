package org.microsauce.scetty.testutils

import java.net.ServerSocket

class PortReservation(val serverSocket: ServerSocket) {

  def releasePort:Int = {
    val result = serverSocket.getLocalPort()
    serverSocket.close()
    result
  }

}
