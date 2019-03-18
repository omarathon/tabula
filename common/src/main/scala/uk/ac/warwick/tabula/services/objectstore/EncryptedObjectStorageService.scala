package uk.ac.warwick.tabula.services.objectstore

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64

import com.google.common.io.ByteSource
import javax.crypto.spec.IvParameterSpec
import javax.crypto.{Cipher, CipherInputStream, SecretKey}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.objectstore.EncryptedObjectStorageService._

object EncryptedObjectStorageService {
  val transformation: String = "AES/CBC/PKCS5Padding"
  val random: SecureRandom = SecureRandom.getInstance("SHA1PRNG")

  val metadataContentLengthKey: String = "realcontentlength"
  val metadataContentTypeKey: String = "realcontenttype"
  val metadataIVKey: String = "encryptioniv"
}

class EncryptedObjectStorageService(delegate: ObjectStorageService, secretKey: SecretKey)
  extends ObjectStorageService with Logging {

  private[this] def decryptionCipher(iv: IvParameterSpec): Cipher = {
    val cipher = Cipher.getInstance(transformation)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
    cipher
  }
  private[this] def decrypt(iv: IvParameterSpec)(is: InputStream): InputStream = new CipherInputStream(is, decryptionCipher(iv))

  private[this] def randomIv: Array[Byte] = {
    val iv = Array.fill[Byte](16){0}
    random.nextBytes(iv)
    iv
  }

  private[this] def encryptionCipher(iv: IvParameterSpec): Cipher = {
    val cipher = Cipher.getInstance(transformation)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
    cipher
  }
  private[this] def encrypt(iv: IvParameterSpec)(is: InputStream): InputStream = new CipherInputStream(is, encryptionCipher(iv))

  private[this] def unwrapMetadata(metadata: ObjectStorageService.Metadata): ObjectStorageService.Metadata =
    metadata.copy(
      contentLength = metadata.userMetadata.get(metadataContentLengthKey).map(_.toLong).getOrElse(metadata.contentLength),
      contentType = metadata.userMetadata.getOrElse(metadataContentTypeKey, metadata.contentType),
      userMetadata = metadata.userMetadata.filterKeys { k => k != metadataContentLengthKey && k != metadataContentTypeKey && k != metadataIVKey }
    )

  private[this] class EncryptedRichByteSource(delegate: RichByteSource) extends RichByteSource {
    override lazy val metadata: Option[ObjectStorageService.Metadata] = delegate.metadata.map(unwrapMetadata)
    override lazy val isEmpty: Boolean = metadata.isEmpty
    override lazy val size: Long = metadata.map(_.contentLength).getOrElse(-1)

    override def openStream(): InputStream = delegate.metadata.map { md =>
      val iv = Base64.getDecoder.decode(md.userMetadata(metadataIVKey).getBytes(StandardCharsets.UTF_8))
      decrypt(new IvParameterSpec(iv))(delegate.openStream())
    }.orNull
  }

  override def fetch(key: String): RichByteSource = new EncryptedRichByteSource(delegate.fetch(key))

  private[this] class EncryptedByteSource(delegate: ByteSource, iv: IvParameterSpec) extends ByteSource {
    override def openStream(): InputStream = Option(delegate.openStream()).map(encrypt(iv)).orNull
    override def isEmpty: Boolean = delegate.isEmpty
  }

  override def push(key: String, in: ByteSource, metadata: ObjectStorageService.Metadata): Unit = {
    val iv = randomIv

    val encrypted = new EncryptedByteSource(in, new IvParameterSpec(iv))
    val encryptedMetadata = ObjectStorageService.Metadata(
      contentLength = encrypted.size(),
      contentType = "application/octet-stream",
      fileHash = metadata.fileHash,
      userMetadata = Map(
        metadataContentLengthKey -> metadata.contentLength.toString,
        metadataContentTypeKey -> metadata.contentType,
        metadataIVKey -> new String(Base64.getEncoder.encode(iv), StandardCharsets.UTF_8)
      ) ++ metadata.userMetadata
    )

    delegate.push(key, encrypted, encryptedMetadata)
  }

  override def keyExists(key: String): Boolean = delegate.keyExists(key)
  override def delete(key: String): Unit = delegate.delete(key)
  override def listKeys(): Stream[String] = delegate.listKeys()

  override def afterPropertiesSet(): Unit = delegate.afterPropertiesSet()
}
