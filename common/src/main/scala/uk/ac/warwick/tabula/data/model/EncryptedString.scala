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

class EncryptedStringUserType extends AbstractBasicUserType[String, Array[Byte]] {
  override val basicType: BinaryType = StandardBasicTypes.BINARY

  override def sqlTypes(): Array[Int] = Array(Types.VARBINARY)

  override val nullObject: String = null
  override val nullValue: Array[Byte] = Array()

  override def convertToObject(input: Array[Byte]): String = {
    if (input.length == 0) {
      nullObject
    } else {
      // Read the IV from the first 16 bytes
      val iv = new IvParameterSpec(input.slice(0, 16))
      val data = ByteSource.wrap(input.slice(16, input.length))

      new DecryptingByteSource(data, EncryptedString.secretKey, iv).asCharSource(StandardCharsets.UTF_8).read()
    }
  }

  override def convertToValue(obj: String): Array[Byte] = {
    val iv = randomIv
    val data = CharSource.wrap(obj).asByteSource(StandardCharsets.UTF_8)

    Array.concat(iv, new EncryptingByteSource(data, EncryptedString.secretKey, new IvParameterSpec(iv)).read())
  }
}