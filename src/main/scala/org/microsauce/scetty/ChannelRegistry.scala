package org.microsauce.scetty

import scala.collection.mutable
import io.netty.channel.Channel
import scala.collection.mutable.ListBuffer
import io.netty.handler.codec.http.FullHttpRequest

/**
 * Created by jboone on 3/17/14.
 */
object ChannelRegistry {

  private val NOT_FOUND = -1

  private val channelMap = new mutable.HashMap[String,ListBuffer[(Request,Channel)]]()

  def register(request:Request,channel:Channel) {
    channelMap.synchronized {
      val channels = channelMap.getOrElse(request.uri, ListBuffer[(Request,Channel)]())
      val index = channelIndex(channel,channels)
      if ( index == NOT_FOUND ) {
        channels.append( (request,channel) )
        channelMap(request.uri) = channels
      }
    }
  }

  def remove(uri:String,channel:Channel) {
    channelMap.synchronized {
      val channels = channelMap.getOrElse(uri, ListBuffer[(Request,Channel)]())
      val index = channelIndex(channel,channels)
      if ( index != NOT_FOUND ) channels.remove(index)
    }
  }

  def findChannels(uri:String):ListBuffer[(Request,Channel)] = {
    channelMap.synchronized {
      channelMap(uri)
    }
  }

  private def channelIndex(channel:Channel,channels:ListBuffer[(Request,Channel)]) = {
    import scala.util.control.Breaks._
    var index = NOT_FOUND
    for ( ndx <- 0 until channels.size ) {
      if ( channels(ndx)._2 == channel ) { index = ndx; break } // TODO break ???
    }
    index
  }

}
