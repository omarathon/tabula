package uk.ac.warwick.tabula.services.objectstore

import java.io.{InputStream, File}

import com.google.common.io.Files
import com.google.common.net.MediaType
import org.jclouds.blobstore.BlobStore
import org.jclouds.blobstore.domain.Blob
import org.jclouds.blobstore.domain.StorageMetadata
import org.jclouds.blobstore.options.ListContainerOptions
import uk.ac.warwick.tabula.helpers.Logging

import scala.collection.JavaConverters._

trait ObjectStorageService {
	def keyExists(key: String): Boolean

	def push(key: String, file: File): Unit

	def pushBlob(blob: Blob): Unit

	def fetch(key: String): Option[InputStream]

	def fetchBlob(key: String): Option[Blob]

	def listKeys(): Stream[String]
}

/**
	* Implementation uses the Apache jclouds library to push and pull data to the cloud store
	*/
class JCloudsObjectStorageServiceImpl(blobStore: BlobStore, objectContainerName: String)
	extends ObjectStorageService with Logging {

	// Create the container if it doesn't exist
	if (!blobStore.containerExists(objectContainerName)) blobStore.createContainerInLocation(null, objectContainerName)

	override def keyExists(key: String) = blobStore.blobExists(objectContainerName, key)

	override def push(key: String, file: File): Unit = benchmark(s"Push file ${file.getAbsolutePath} (size: ${file.length()} bytes) for key $key", level = Logging.Level.Debug) {
		val payload = Files.asByteSource(file)
		val blob = blobStore.blobBuilder(key)
			.payload(payload)
			.contentDisposition(key)
			.contentLength(payload.size())
			.contentType(MediaType.OCTET_STREAM.toString)
			.build()

		blobStore.putBlob(objectContainerName, blob)
	}

	override def pushBlob(blob: Blob): Unit = benchmark(s"Push blob (size: ${blob.getMetadata.getSize} bytes) for key ${blob.getMetadata.getName}", level = Logging.Level.Debug) {
		blobStore.putBlob(objectContainerName, blob)
	}

	override def fetch(key: String): Option[InputStream] = benchmark(s"Fetch key $key", level = Logging.Level.Debug) {
		val blob = Option(blobStore.getBlob(objectContainerName, key))
		val payload = blob.map(_.getPayload)
		payload.map(p => p.openStream)
	}

	override def fetchBlob(key: String): Option[Blob] = benchmark(s"Fetch blob $key", level = Logging.Level.Debug) {
		Option(blobStore.getBlob(objectContainerName, key))
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
