package uk.ac.warwick.tabula.services.objectstore

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Base64

import com.google.common.io.ByteSource
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import uk.ac.warwick.tabula.helpers.AESEncryption._
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.objectstore.EncryptedObjectStorageService._

import scala.concurrent.Future

object EncryptedObjectStorageService {
  val metadataContentLengthKey: String = "realcontentlength"
  val metadataContentTypeKey: String = "realcontenttype"
  val metadataIVKey: String = "encryptioniv"
}

class EncryptedObjectStorageService(delegate: ObjectStorageService, secretKey: SecretKey)
  extends ObjectStorageService with Logging {

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
    override val encrypted: Boolean = true

    override def openStream(): InputStream = delegate.metadata.map { md =>
      val iv = Base64.getDecoder.decode(md.userMetadata(metadataIVKey).getBytes(StandardCharsets.UTF_8))
      decrypt(secretKey, new IvParameterSpec(iv))(delegate.openStream())
    }.orNull
  }

  override def fetch(key: String): Future[RichByteSource] = delegate.fetch(key).map(new EncryptedRichByteSource(_))

  override def push(key: String, in: ByteSource, metadata: ObjectStorageService.Metadata): Future[Unit] =
    Future {
      val iv = randomIv

      val encrypted = new EncryptingByteSource(in, secretKey, new IvParameterSpec(iv))
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

      (encrypted, encryptedMetadata)
    }.flatMap { case (encrypted, encryptedMetadata) => delegate.push(key, encrypted, encryptedMetadata) }

  override def keyExists(key: String): Future[Boolean] = delegate.keyExists(key)
  override def delete(key: String): Future[Unit] = delegate.delete(key)
  override def listKeys(): Future[Stream[String]] = delegate.listKeys()

  override def afterPropertiesSet(): Unit = delegate.afterPropertiesSet()
}
