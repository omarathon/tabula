package uk.ac.warwick.tabula.services.objectstore

import java.io.{File, InputStream}
import java.util.Properties

import com.google.common.net.MediaType
import org.jclouds.ContextBuilder
import org.jclouds.blobstore.domain.StorageMetadata
import org.jclouds.blobstore.options.ListContainerOptions
import org.jclouds.blobstore.{BlobStore, BlobStoreContext}
import org.jclouds.filesystem.reference.FilesystemConstants
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ScalaFactoryBean
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile

import scala.collection.JavaConverters._

object ObjectStorageService {
	case class Metadata(
		contentLength: Long,
		contentType: String,
		fileHash: Option[String]
	)
}

trait ObjectStorageService {
	def keyExists(key: String): Boolean

	def fetch(key: String): Option[InputStream]

	def metadata(key: String): Option[ObjectStorageService.Metadata]

	/**
		* Combines calls to fetch() and metadata()
		*/
	def renderable(key: String): Option[RenderableFile] =
		if (keyExists(key)) {
			Some(new RenderableFile {
				override val file: Option[File] = None
				override val filename: String = key

				private lazy val fileMetadata = metadata(key)

				override lazy val contentLength: Option[Long] = fileMetadata.map(_.contentLength)

				override lazy val contentType: String = fileMetadata.map(_.contentType).getOrElse(MediaType.OCTET_STREAM.toString)

				override def inputStream: InputStream = fetch(key).orNull
			})
		} else None

	def push(key: String, in: InputStream, metadata: ObjectStorageService.Metadata): Unit

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

	// To be removed once everything's been transferred over
	@Value("${filesystem.attachment.dir}") var legacyAttachmentsDirectory: File = _

	private def createFilesystemBlobContext(): BlobStoreContext = {
		val properties = {
			val p = new Properties
			p.setProperty(FilesystemConstants.PROPERTY_BASEDIR, Wire.property("${objectstore.filesystem.baseDir}"))
			p
		}

		ContextBuilder.newBuilder("filesystem")
			.overrides(properties)
			.buildView(classOf[BlobStoreContext])
	}

	private def createSwiftBlobContext(): BlobStoreContext = {
		val endpoint = Wire.property("${objectstore.swift.endpoint}")
		val username = Wire.property("${objectstore.swift.username}")
		val password = Wire.property("${objectstore.swift.password}")

		ContextBuilder.newBuilder("openstack-swift")
			.endpoint(endpoint)
			.credentials(s"LDAP_$username:$username", password)
			.buildView(classOf[BlobStoreContext])
	}

	override def createInstance(): ObjectStorageService = {
		blobStoreContext = providerType match {
			case "filesystem" => createFilesystemBlobContext()
			case "swift" => createSwiftBlobContext()
			case "transient" => ContextBuilder.newBuilder("transient").buildView(classOf[BlobStoreContext])
			case _ => throw new IllegalArgumentException(s"Invalid provider type $providerType")
		}

		new LegacyAwareObjectStorageService(
			defaultService = new BlobStoreObjectStorageService(blobStoreContext.getBlobStore, containerName),
			legacyService = new LegacyFilesystemObjectStorageService(legacyAttachmentsDirectory)
		)
	}

	override def destroy(): Unit = blobStoreContext.close()

}

trait ObjectStorageServiceComponent {
	def objectStorageService: ObjectStorageService
}

trait AutowiringObjectStorageServiceComponent extends ObjectStorageServiceComponent {
	var objectStorageService = Wire[ObjectStorageService]
}

/**
	* Implementation uses the Apache jclouds library to push and pull data to the cloud store
	*/
class BlobStoreObjectStorageService(blobStore: BlobStore, objectContainerName: String)
	extends ObjectStorageService with Logging {

	// Create the container if it doesn't exist
	if (!blobStore.containerExists(objectContainerName)) blobStore.createContainerInLocation(null, objectContainerName)

	override def keyExists(key: String) = blobStore.blobExists(objectContainerName, key)

	private def blob(key: String) = key.maybeText.flatMap { k => Option(blobStore.getBlob(objectContainerName, k)) }

	override def push(key: String, in: InputStream, metadata: ObjectStorageService.Metadata): Unit = benchmark(s"Push file for key $key", level = Logging.Level.Debug) {
		Assert.notNull(key, "Key must be defined")

		val blob = blobStore.blobBuilder(key)
			.payload(in)
			.contentDisposition(key)
			.contentType(metadata.contentType)
			.contentLength(metadata.contentLength)
			.userMetadata(metadata.fileHash.map { h => "shahex" -> h }.toMap.asJava)
		  .build()

		blobStore.putBlob(objectContainerName, blob)
	}

	override def fetch(key: String): Option[InputStream] = benchmark(s"Fetch key $key", level = Logging.Level.Debug) {
		blob(key).map { _.getPayload.openStream() }
	}

	override def metadata(key: String): Option[ObjectStorageService.Metadata] = benchmark(s"Metadata key $key", level = Logging.Level.Debug) {
		blob(key).map { blob =>
			val contentMetadata = blob.getPayload.getContentMetadata
			val metadata = blob.getMetadata

			ObjectStorageService.Metadata(
				contentLength = contentMetadata.getContentLength,
				contentType = contentMetadata.getContentType,
				fileHash = metadata.getUserMetadata.get("shahex").maybeText
			)
		}
	}

	def delete(key: String): Unit = benchmark(s"Deleting key $key", level = Logging.Level.Debug) {
		blobStore.removeBlob(objectContainerName, key)
	}

	override def listKeys(): Stream[String] = benchmark("List keys", level = Logging.Level.Debug) {
		def list(nextMarker: Option[String], accumulator: Stream[_ <: StorageMetadata]): Stream[_ <: StorageMetadata] = nextMarker match {
			case None => // No more results
				accumulator

			case Some(marker) =>
				accumulator.append({
					val nextResults = blobStore.list(objectContainerName, ListContainerOptions.Builder.afterMarker(marker))
					list(Option(nextResults.getNextMarker), nextResults.asScala.toStream)
				})
		}

		val firstResults = blobStore.list(objectContainerName)
		list(Option(firstResults.getNextMarker), firstResults.asScala.toStream).map {
			_.getName
		}
	}
}
