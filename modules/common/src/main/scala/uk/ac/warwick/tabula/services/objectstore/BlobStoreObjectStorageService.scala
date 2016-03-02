package uk.ac.warwick.tabula.services.objectstore

import java.io.InputStream

import com.google.common.io.ByteSource
import org.jclouds.blobstore.BlobStoreContext
import org.jclouds.blobstore.domain.{Blob, StorageMetadata}
import org.jclouds.blobstore.options.{PutOptions, ListContainerOptions}
import org.springframework.beans.factory.InitializingBean
import org.springframework.util.Assert
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.collection.JavaConverters._

/**
	* Implementation uses the Apache jclouds library to push and pull data to the cloud store
	*/
class BlobStoreObjectStorageService(blobStoreContext: BlobStoreContext, objectContainerName: String)
	extends ObjectStorageService with Logging with InitializingBean {

	protected lazy val blobStore = blobStoreContext.getBlobStore

	override def afterPropertiesSet(): Unit = {
		// Create the container if it doesn't exist
		if (!blobStore.containerExists(objectContainerName)) blobStore.createContainerInLocation(null, objectContainerName)
	}

	override def keyExists(key: String) = blobStore.blobExists(objectContainerName, key)

	private def blob(key: String) = key.maybeText.flatMap { k => Option(blobStore.getBlob(objectContainerName, k)) }

	override def push(key: String, in: ByteSource, metadata: ObjectStorageService.Metadata): Unit = benchmark(s"Push file for key $key", level = Logging.Level.Debug) {
		Assert.notNull(key, "Key must be defined")

		val blob = blobStore.blobBuilder(key)
			.payload(in)
			.contentDisposition(key)
			.contentType(metadata.contentType)
			.contentLength(metadata.contentLength)
			.userMetadata(metadata.fileHash.map { h => "shahex" -> h }.toMap.asJava)
			.build()

		// TAB-4144 Use large object support for anything over 50mb
		blobStore.putBlob(objectContainerName, blob, PutOptions.Builder.multipart(metadata.contentLength > 50 * 1024 * 1024))
	}

	/**
		* Overridden in SwiftObjectStorageService to support multipart
		*/
	protected def putBlob(blob: Blob, metadata: ObjectStorageService.Metadata) = blobStore.putBlob(objectContainerName, blob)

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
