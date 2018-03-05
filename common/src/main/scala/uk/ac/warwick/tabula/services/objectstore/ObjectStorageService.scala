package uk.ac.warwick.tabula.services.objectstore

import java.io.{File, InputStream}
import java.util.Properties

import com.google.common.io.ByteSource
import com.google.common.net.MediaType
import org.jclouds.ContextBuilder
import org.jclouds.blobstore.domain.{Blob, BlobMetadata}
import org.jclouds.blobstore.{BlobStore, BlobStoreContext, TransientApiMetadata}
import org.jclouds.filesystem.FilesystemApiMetadata
import org.jclouds.filesystem.reference.FilesystemConstants
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule
import org.jclouds.openstack.swift.v1.SwiftApiMetadata
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ScalaFactoryBean
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.util.files.impl.AbstractBlobBackedFileData

import scala.collection.JavaConverters._

object ObjectStorageService {
	case class Metadata(
		contentLength: Long,
		contentType: String,
		fileHash: Option[String]
	)

	object Metadata {
		def apply(blobMetadata: BlobMetadata): Metadata = Metadata(
			contentLength = blobMetadata.getSize.longValue,
			contentType = blobMetadata.getContentMetadata.getContentType,
			fileHash = blobMetadata.getUserMetadata.get("shahex").maybeText
		)
	}
}

trait RichByteSource extends ByteSource {
	def metadata: Option[ObjectStorageService.Metadata]
}

object RichByteSource {
	def apply(fetchBlob: => Option[Blob], fetchMetadata: => Option[BlobMetadata]): RichByteSource = new BlobBackedByteSource(fetchBlob, fetchMetadata)
	def wrap(source: ByteSource, md: Option[ObjectStorageService.Metadata]): RichByteSource = new RichByteSource {
		override lazy val metadata: Option[ObjectStorageService.Metadata] = md
		override def openStream(): InputStream = source.openStream()
		override lazy val size: Long = source.size()
		override lazy val isEmpty: Boolean = source.isEmpty
		override def read(): Array[Byte] = source.read()
	}
	def empty: RichByteSource = new BlobBackedByteSource(None, None)
}

class ReadOnlyBlobBackedFileData(blobStore: BlobStore, containerName: String, blobName: String) extends AbstractBlobBackedFileData(blobStore, containerName, blobName) {
	override def overwrite(in: ByteSource) = throw new UnsupportedOperationException
}

class BlobBackedRichByteSource(blobStore: BlobStore, containerName: String, blobName: String, fetchMetadata: => Option[BlobMetadata]) extends RichByteSource {
	private lazy val byteSource = new ReadOnlyBlobBackedFileData(blobStore, containerName, blobName).asByteSource()

	override lazy val metadata: Option[ObjectStorageService.Metadata] = fetchMetadata.map(ObjectStorageService.Metadata.apply)
	override lazy val isEmpty: Boolean = metadata.isEmpty
	override lazy val size: Long = metadata.map(_.contentLength).getOrElse(-1)

	override def openStream(): InputStream = try byteSource.openStream() catch {
		// BlobBackedByteSource::openStream doesn't check whether a Blob's Payload is non-null
		case e: NullPointerException => null
	}
	override def slice(offset: Long, length: Long): ByteSource = byteSource.slice(offset, length)
}

private[objectstore] class BlobBackedByteSource(fetchBlob: => Option[Blob], fetchMetadata: => Option[BlobMetadata]) extends RichByteSource {
	override lazy val metadata: Option[ObjectStorageService.Metadata] =
		fetchMetadata.map(ObjectStorageService.Metadata.apply)

	override def openStream(): InputStream = fetchBlob.map(_.getPayload.openStream()).orNull

	override lazy val isEmpty: Boolean = metadata.isEmpty
	override lazy val size: Long = metadata.map(_.contentLength).getOrElse(-1)
}

trait ObjectStorageService extends InitializingBean {
	def keyExists(key: String): Boolean
	def fetch(key: String): RichByteSource

	/**
		* Combines calls to fetch() and metadata()
		*/
	def renderable(key: String, fileName: Option[String]): Option[RenderableFile] =
		Option(fetch(key)).filterNot(_.isEmpty).map { source =>
			new RenderableFile {
				override val filename: String = fileName.getOrElse(key)

				private lazy val fileMetadata = source.metadata

				override lazy val contentLength: Option[Long] = fileMetadata.map(_.contentLength)

				override lazy val contentType: String = fileMetadata.map(_.contentType).getOrElse(MediaType.OCTET_STREAM.toString)

				override def inputStream: InputStream = source.openStream()
			}
		}

	def push(key: String, in: ByteSource, metadata: ObjectStorageService.Metadata): Unit
	def delete(key: String): Unit
	def listKeys(): Stream[String]
}

@Service
class ObjectStorageServiceFactoryBean extends ScalaFactoryBean[ObjectStorageService] {

	// As this a singleton, this reference will be retained by the bean factory,
	// so don't need to worry about it going away
	@transient private var blobStoreContext: BlobStoreContext = _

	@Value("${objectstore.container}") var containerName: String = _
	@Value("${objectstore.provider}") var providerType: String = _

	// This defaults to an empty string
	@Value("${filesystem.attachment.dir:}") var legacyAttachmentsDirectory: String = _

	private def createFilesystemBlobContext(): ContextBuilder = {
		val properties = {
			val p = new Properties
			p.setProperty(FilesystemConstants.PROPERTY_BASEDIR, Wire.property("${objectstore.filesystem.baseDir}"))
			p
		}

		ContextBuilder.newBuilder(new FilesystemApiMetadata)
			.overrides(properties)
	}

	private def createSwiftBlobContext(): ContextBuilder = {
		val endpoint = Wire.property("${objectstore.swift.endpoint}")
		val username = Wire.property("${objectstore.swift.username}")
		val password = Wire.property("${objectstore.swift.password}")

		ContextBuilder.newBuilder(new SwiftApiMetadata)
			.endpoint(endpoint)
			.credentials(s"LDAP_$username:$username", password)
	}

	override def createInstance(): ObjectStorageService = {
		val contextBuilder = (providerType match {
			case "filesystem" => createFilesystemBlobContext()
			case "swift" => createSwiftBlobContext()
			case "transient" => ContextBuilder.newBuilder(new TransientApiMetadata)
			case _ => throw new IllegalArgumentException(s"Invalid provider type $providerType")
		}).modules(Seq(new SLF4JLoggingModule).asJava)

		blobStoreContext = contextBuilder.buildView(classOf[BlobStoreContext])

		val blobStoreService = new BlobStoreObjectStorageService(blobStoreContext, containerName)

		legacyAttachmentsDirectory.maybeText.map(new File(_)) match {
			case Some(dir) => new LegacyAwareObjectStorageService(
				defaultService = blobStoreService,
				legacyService = new LegacyFilesystemObjectStorageService(dir)
			)

			case _ => blobStoreService
		}
	}

	override def destroy(): Unit = blobStoreContext.close()

}

trait ObjectStorageServiceComponent {
	def objectStorageService: ObjectStorageService
}

trait AutowiringObjectStorageServiceComponent extends ObjectStorageServiceComponent {
	var objectStorageService: ObjectStorageService = Wire[ObjectStorageService]
}