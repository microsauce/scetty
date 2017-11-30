package org.microsauce.scetty

import org.microsauce.scetty.util.ScettyUtil.coalesce

import scala.collection.mutable.ListBuffer

/**
  * Build a ScettyContext for your Scetty server.
  */
object ScettyContextBuilder {

  def create: ScettyContextBuilder = {
    new ScettyContextBuilder()
  }
}


class ScettyContextBuilder {

  var _inetAddress: String = ScettyDefaults.inetAddressName
  var _port: Int = ScettyDefaults.port
  var _maxInitialLineLength: Int = ScettyDefaults.maxInitialLineLength
  var _maxHeaderSize: Int = ScettyDefaults.maxHeaderSize
  var _maxChunkSize: Int = ScettyDefaults.maxChunkSize
  var _maxContentLength: Int = ScettyDefaults.maxContentLength
  var _dataFactoryMinSize: Long = ScettyDefaults.dataFactoryMinSize

  var _routers: ListBuffer[Router] = new ListBuffer[Router]

  var _ssl: Boolean = ScettyDefaults.ssl
  var _sslKeystore: String = ScettyDefaults.sslKeystore
  var _keypass: String = ScettyDefaults.keypass

  def prependRouter(router: Router): ScettyContextBuilder = {
    _routers.insert(0, router)
    this
  }

  def addRouter(router: Router): ScettyContextBuilder = {
    _routers += router
    this
  }

  /**
    * Set the address on which to bind the server
    */
  def inetAddress(inetAddress: String): ScettyContextBuilder = {
    this._inetAddress = inetAddress
    this
  }

  /**
    * Set the port on which to bind the server; default is 80
    */
  def port(port: Int): ScettyContextBuilder = {
    this._port = port
    this
  }

  /**
    * Set the HttpRequestDecoder maxInitialLineLength; default is 4096
    */
  def maxInitialLineLength(maxInitialLineLength: Int): ScettyContextBuilder = {
    this._maxInitialLineLength = maxInitialLineLength
    this
  }

  /**
    * Set the HttpRequestDecoder maxHeaderSize; default is 8192
    *
    * @param maxHeaderSize
    * @return
    */
  def maxHeaderSize(maxHeaderSize: Int): ScettyContextBuilder = {
    this._maxHeaderSize = maxHeaderSize
    this
  }

  /**
    * Set the HttpRequestDecoder maxChunkSize; default is 8192
    *
    * @param maxChunkSize
    * @return
    */
  def maxChunkSize(maxChunkSize: Int): ScettyContextBuilder = {
    this._maxChunkSize = maxChunkSize
    this
  }

  /**
    * Set the HttpObjectAggregator maxContentLength value:  default is 65536
    *
    * @param maxContentLength
    */
  def maxContentLength(maxContentLength: Int): ScettyContextBuilder = {
    this._maxContentLength = maxContentLength
    this
  }

  /**
    * Set the DefaultHttpDataFactory size:  default is DefaultHttpDataFactory.MINSIZE
    *
    * @param dataFactoryMinSize
    * @return
    */
  def dataFactoryMinSize(dataFactoryMinSize: Long): ScettyContextBuilder = {
    this._dataFactoryMinSize = dataFactoryMinSize
    this
  }

  /**
    * Set the SSL flag - default is false
    */
  def ssl(sslEnabled: Boolean): ScettyContextBuilder = {
    this._ssl = sslEnabled
    this
  }

  /**
    * Set the path to java keystore - used to initialize the javax SSLContext
    */
  def keystore(keystore: String): ScettyContextBuilder = {
    this._sslKeystore = keystore
    this
  }

  /**
    * Set the java keystore password - used to initialize the javax SSLContext
    */
  def keypass(keypass: String): ScettyContextBuilder = {
    this._keypass = keypass
    this
  }

  def build: ScettyContext = {
    val result = new ScettyContext()

    result.inetAddress = _inetAddress
    result.keypass = _keypass
    result.maxChunkSize = _maxChunkSize
    result.maxContentLength = _maxContentLength
    result.maxHeaderSize = _maxHeaderSize
    result.maxInitialLineLength = _maxInitialLineLength
    result.port = _port
    result.ssl = _ssl
    result.sslKeystore = _sslKeystore
    result.routers = _routers

    result
  }

}
