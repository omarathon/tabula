package uk.ac.warwick.tabula.data.model

import java.nio.charset.StandardCharsets
import java.sql.Types
import java.util.Base64

import com.google.common.io.{ByteSource, CharSource}
import javax.crypto.SecretKey
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import org.hibernate.`type`.{BinaryType, StandardBasicTypes}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.AESEncryption._

object EncryptedString {
  // Base64-encoded AES encryption key. Use ./gradlew generateEncryptionKey to generate one
  var encryptionKey: String = Wire.property("${tabula.database.encryptionKey}")

  lazy val secretKey: SecretKey = {
    val decodedKey: Array[Byte] = Base64.getDecoder.decode(encryptionKey.getBytes(StandardCharsets.UTF_8))
    new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES")
  }
}

class EncryptedString(encrypted: Array[Byte]) extends CharSequence {
  override def length(): Int = toString.length
  override def charAt(index: Int): Char = toString.charAt(index)
  override def subSequence(start: Int, end: Int): CharSequence = toString.subSequence(start, end)

  override def equals(other: Any): Boolean = other match {
    case that: CharSequence => toString == that.toString
    case _ => false
  }

  override def hashCode(): Int = toString.hashCode()

  override lazy val toString: String = {
    // Read the IV from the first 16 bytes
    val iv = new IvParameterSpec(encrypted.slice(0, 16))
    val data = ByteSource.wrap(encrypted.slice(16, encrypted.length))

    new DecryptingByteSource(data, EncryptedString.secretKey, iv).asCharSource(StandardCharsets.UTF_8).read()
  }
}

class EncryptedStringUserType extends AbstractBasicUserType[CharSequence, Array[Byte]] {
  override val basicType: BinaryType = StandardBasicTypes.BINARY

  override def sqlTypes(): Array[Int] = Array(Types.VARBINARY)

  override val nullObject: CharSequence = null
  override val nullValue: Array[Byte] = Array()

  override def convertToObject(input: Array[Byte]): CharSequence =
    if (input.length == 0) nullObject
    else new EncryptedString(input)

  override def convertToValue(obj: CharSequence): Array[Byte] = {
    val iv = randomIv
    val data = CharSource.wrap(obj).asByteSource(StandardCharsets.UTF_8)

    Array.concat(iv, new EncryptingByteSource(data, EncryptedString.secretKey, new IvParameterSpec(iv)).read())
  }
}