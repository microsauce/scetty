package org.microsauce.scetty

/**
 * Created by jboone on 1/30/14.
 */
object Test extends App {

  val CHUNK_LEN = 4096

  val LEN = 22222
  println("Chunk it up . . .")
  val array = new Array[Byte](LEN)
  for (i <- 0 until LEN) array(i) = 1.toByte
println(s"array length: ${array.length}")
  val cookies = chunkSession(array)
  var total = 0
  for ( c<-cookies ) {total+=c.length;println(s"len: ${c.length}")}
  println(s"total: $total")

  println("Fusion . . .")
  println(s"reassembled length: ${assembleSession(cookies).length}")

  val strBuf = new StringBuilder()
  for(j<-0 until LEN) strBuf.append("a")
  println(s"\nstring bytes: ${strBuf.toString.getBytes("utf-8")}")
  val strChunks = chunkedSessionString(strBuf.toString)
  var scTotal=0
  for (sc<-strChunks) scTotal+=sc.length
  println(s"total length of all chunks: ${scTotal}")
  var scbTotal=0
  for (sc<-strChunks) scbTotal+=sc.getBytes("utf-8").length
  println(s"total length of all chunks as bytes: ${scbTotal}")

  private def chunkSession(session:Array[Byte]) = {
    val sessLen = session.length
    val quotient = session.length.toDouble / 4096.toDouble
    val chunks = Math.ceil(quotient).toInt
    val cookies = new Array[Array[Byte]](chunks)
    var start,end=0
    var accumulatedBytes = 0
    for ( ndx <- 0 until chunks ) {
      end = if ( sessLen-accumulatedBytes<4096 ) sessLen
      else start+4096
      println(s"chunk number $ndx . . . $start -> $end")
      cookies(ndx) = session.slice(start,end)
      start = end
      accumulatedBytes+=4096
    }
    cookies
  }


  private def assembleSession(chunks:Array[Array[Byte]]) = {
    val len = chunks.foldLeft(0){(total,arr)=>total+arr.length}
    var accumulatedBytes = 0
    val session = new Array[Byte](len)
    var start = 0
    var end = 0
    for ( c<-chunks ) {
println(s"\tthis chunk len: ${c.length}")
      end = if ( len-accumulatedBytes<4096 ) len
      else start+4096
println(s"\t start -> end: $start -> $end")
      c.copyToArray(session,start,end)
      start = end
      accumulatedBytes+=4096
    }
    session
  }


  private def chunkedSessionString(session:String) = {
    val sessLen = session.length
    val quotient = session.length.toDouble / CHUNK_LEN.toDouble
    val chunks = Math.ceil(quotient).toInt
    val cookies = new Array[String](chunks)//new Array[Array[Byte]](chunks)
    var start,end=0
    var accumulatedBytes = 0
    for ( ndx <- 0 until chunks ) {
      end = if ( sessLen-accumulatedBytes<CHUNK_LEN ) sessLen
      else start+CHUNK_LEN
      cookies(ndx) = session.substring(start,end)// slice(start,end)
      start = end
      accumulatedBytes+=CHUNK_LEN
    }
    cookies
  }

  private def assembleSessionString(chunks:Array[String]) = {
    val session = new StringBuilder()//new Array[Byte](len)
    for ( c<-chunks ) {
      session.append(c)
    }
    session.toString
  }



}
