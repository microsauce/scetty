package org.microsauce.scetty

/**
 * A mapping of common file extensions to their mime type id.
 */
object MimeTable {
  
  import scala.io.Source
  
  private val map = scala.collection.mutable.Map[String,String]()
  private val fileExtensionPattern = """.+\.([a-zA-Z0-9]+)""".r
  private val defaultMimeType = "application/octet-stream"
  
  def add(line:String) {
    val tokens = line.split(" ")
    val mimeString = tokens.head
    val fileExtensions = tokens.tail
    fileExtensions.foreach(map(_) = mimeString)
  }
  
  def getType(path:String) = path match {
    case fileExtensionPattern(mtch) => map.getOrElse(mtch, defaultMimeType)
    case _ => defaultMimeType
  }
  
  def getTypeByExtension(ext:String) = {
    val typ = map(ext)
    if ( typ == null ) defaultMimeType
    else typ
  }
  
  //
  // initialize the mime table
  //
  private val mtis = MimeTable.getClass.getClassLoader.getResourceAsStream("mime.table")
  Source.fromInputStream(mtis).getLines.foreach(add(_))
}
