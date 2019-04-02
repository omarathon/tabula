package uk.ac.warwick.tabula.services.objectstore

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64

import com.google.common.hash.Hashing
import com.google.common.io.ByteSource
import javax.crypto.spec.SecretKeySpec
import javax.crypto.{KeyGenerator, SecretKey}
import org.jclouds.ContextBuilder
import org.jclouds.blobstore.{BlobStoreContext, TransientApiMetadata}
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule
import org.scalatest.time.{Millis, Seconds, Span}
import uk.ac.warwick.tabula.{Mockito, TestBase}

import scala.collection.JavaConverters._

class EncryptedObjectStorageServiceTest extends TestBase with Mockito {

  private[this] trait SecretKeyFixture {
    val secretKey: SecretKey
  }

  private[this] trait GeneratedSecretKeyFixture extends SecretKeyFixture {
    lazy val secretKey: SecretKey = {
      val keyGenerator = KeyGenerator.getInstance("AES")
      val random = SecureRandom.getInstance("SHA1PRNG")
      keyGenerator.init(256, random)
      keyGenerator.generateKey()
    }
  }

  private[this] trait AssignedSecretKeyFixture extends SecretKeyFixture {
    lazy val secretKey: SecretKey = {
      val encryptionKey: String = "ftPWWb0Md9M+Mm7tJLMUsvQODvm3NJQ9ZmVTLSI/HRE="
      val decodedKey: Array[Byte] = Base64.getDecoder.decode(encryptionKey.getBytes(StandardCharsets.UTF_8))
      new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES")
    }
  }

  private[this] trait TransientObjectStorageServiceFixture {
    self: SecretKeyFixture =>

    val blobStoreContext: BlobStoreContext =
      ContextBuilder.newBuilder(new TransientApiMetadata)
        .modules(Seq(new SLF4JLoggingModule).asJava)
        .buildView(classOf[BlobStoreContext])

    val objectStorageService: ObjectStorageService = new BlobStoreObjectStorageService(blobStoreContext, "uk.ac.warwick.blobs")
    lazy val encryptedObjectStorageService = new EncryptedObjectStorageService(objectStorageService, secretKey)
    encryptedObjectStorageService.afterPropertiesSet()
  }

  @Test
  def encryptAndDecrypt(): Unit = new TransientObjectStorageServiceFixture with GeneratedSecretKeyFixture {
    val byteSource: ByteSource = new ByteSource {
      override def openStream(): InputStream = getClass.getResourceAsStream("/attachment1.docx")
    }

    val sha256: String = byteSource.hash(Hashing.sha256()).toString

    encryptedObjectStorageService.push("encrypted", byteSource, ObjectStorageService.Metadata(
      contentLength = 14949,
      contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      fileHash = None
    )).futureValue

    val fetchedByteSource: RichByteSource = encryptedObjectStorageService.fetch("encrypted").futureValue
    fetchedByteSource.isEmpty should be (false)

    fetchedByteSource.hash(Hashing.sha256()).toString should be (sha256)
    fetchedByteSource.size() should be (14949)
  }

  @Test
  def storeMetadataCorrectly(): Unit = new TransientObjectStorageServiceFixture with AssignedSecretKeyFixture {
    val byteSource: ByteSource = new ByteSource {
      override def openStream(): InputStream = getClass.getResourceAsStream("/attachment1.docx")
    }

    encryptedObjectStorageService.push("encrypted", byteSource, ObjectStorageService.Metadata(
      contentLength = 14949,
      contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      fileHash = None
    )).futureValue

    encryptedObjectStorageService.fetch("encrypted").futureValue.metadata should be (Some(ObjectStorageService.Metadata(
      contentLength = 14949,
      contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      fileHash = None,
      userMetadata = Map()
    )))

    // Directly on the encrypted metadata
    val metadata: Option[ObjectStorageService.Metadata] = objectStorageService.fetch("encrypted").futureValue.metadata
    metadata should be ('defined)
    metadata.get.contentLength should be (14960)
    metadata.get.contentType should be ("application/octet-stream")
  }

}
