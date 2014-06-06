package org.microsauce.scetty

import javax.net.ssl.SSLContext
import java.security.KeyStore
import java.io.ByteArrayInputStream
import org.apache.commons.codec.binary.Base64
import javax.net.ssl.KeyManagerFactory
import java.io.FileInputStream
import java.io.File

object SSLProviderFactory {
  
  def getProvider(keyFile:String,keyPass:String) = {
    val sslContext = SSLContext.getInstance("TLS")
  
    val keyStore = KeyStore.getInstance("JKS")
    keyStore.load(new FileInputStream(new File(keyFile)),keyPass.toCharArray)
    val kmf = KeyManagerFactory.getInstance("SunX509") 
    kmf.init(keyStore,keyPass.toCharArray)
    sslContext.init(kmf.getKeyManagers,null,null)
    () => sslContext
  }

}