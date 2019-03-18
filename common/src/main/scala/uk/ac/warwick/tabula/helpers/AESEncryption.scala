package uk.ac.warwick.tabula.helpers

import java.io.InputStream
import java.security.SecureRandom

import javax.crypto.spec.IvParameterSpec
import javax.crypto.{Cipher, CipherInputStream, SecretKey}

object AESEncryption {
  private val transformation: String = "AES/CBC/PKCS5Padding"
  private val random: SecureRandom = SecureRandom.getInstance("SHA1PRNG")

  private def decryptionCipher(secretKey: SecretKey, iv: IvParameterSpec): Cipher = {
    val cipher = Cipher.getInstance(transformation)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
    cipher
  }
  def decrypt(secretKey: SecretKey, iv: IvParameterSpec)(is: InputStream): InputStream = new CipherInputStream(is, decryptionCipher(secretKey, iv))

  def randomIv: Array[Byte] = {
    val iv = Array.fill[Byte](16){0}
    random.nextBytes(iv)
    iv
  }

  private def encryptionCipher(secretKey: SecretKey, iv: IvParameterSpec): Cipher = {
    val cipher = Cipher.getInstance(transformation)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
    cipher
  }
  def encrypt(secretKey: SecretKey, iv: IvParameterSpec)(is: InputStream): InputStream = new CipherInputStream(is, encryptionCipher(secretKey, iv))
}
