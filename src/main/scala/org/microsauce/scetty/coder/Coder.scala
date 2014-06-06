package org.microsauce.scetty.coder

import org.apache.commons.codec.binary.Base64
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import java.math.BigInteger

/**
 * Provides commonly used coder objects/providers.
 *
 * - base64Coder - http safe base 64 encoding
 * - aesCoder - aes encryption coder
 */
object Coder {
  
  lazy val base64Coder = new Coder[Array[Byte],String](
    data => Base64.encodeBase64URLSafeString(data),
    str => Base64.decodeBase64(str.getBytes("utf-8"))
  )

  lazy val base64Coder2 = new Coder[Array[Byte],Array[Byte]](
    data => Base64.encodeBase64URLSafeString(data).getBytes("utf-8"),
    data => Base64.decodeBase64(data)
  )

  def aesCoder(key:String,iv:String,algorithm:String) = {
    val bKey = decodeHex(key)
    val bIv  = decodeHex(iv)

    val spec = new SecretKeySpec(bKey, "AES")
    val encryptor = Cipher.getInstance(algorithm)
    val decryptor = Cipher.getInstance(algorithm)
    encryptor.init(Cipher.ENCRYPT_MODE, spec, new IvParameterSpec(bIv, 0, encryptor.getBlockSize()))
    decryptor.init(Cipher.DECRYPT_MODE, spec, new IvParameterSpec(bIv, 0, decryptor.getBlockSize()))

    new Coder[Array[Byte],Array[Byte]](
      data => encryptor.doFinal(data),
      data => decryptor.doFinal(data)
    )
  }

  private def decodeHex(encoded:String):Array[Byte] = {
    var decoded = (new BigInteger(encoded, 16)).toByteArray
	  if ( decoded(0) == 0 ) {
	    def tmp = new Array[Byte](decoded.length - 1)
	    System.arraycopy(decoded, 1, tmp, 0, tmp.length)
	    decoded = tmp
	  }
	  return decoded
  }
}

/**
 * The Coder class encapsulates a pair of functions used to encode
 * and decode their inputs.  A Coder instance can be composed with another
 * coder instance to produced a third coder instance.
 */
class Coder[A,B](_encode:A=>B,_decode:B=>A) {

  /**
   * Encode the input.
   */
  val encode = _encode

  /**
   * Decode the input.
   */
  val decode = _decode
  
  /**
   * Compose a new coder instance from this and the given input
   */
  def compose(coder:Coder[B,A]) = {
    new Coder[B,B](encode.compose(coder.encode),coder.decode.compose(decode))
  }
  
}