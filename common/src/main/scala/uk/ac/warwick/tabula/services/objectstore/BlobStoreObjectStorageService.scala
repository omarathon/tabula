package uk.ac.warwick.tabula.services.objectstore

import com.google.common.io.ByteSource
import org.jclouds.blobstore.domain.{Blob, StorageMetadata}
import org.jclouds.blobstore.options.{ListContainerOptions, PutOptions}
import org.jclouds.blobstore.strategy.internal.MultipartUploadSlicingAlgorithm
import org.jclouds.blobstore.{BlobStore, BlobStoreContext}
import org.jclouds.io.internal.BasePayloadSlicer
import org.springframework.beans.factory.InitializingBean
import org.springframework.util.Assert
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
  * Implementation uses the Apache jclouds library to push and pull data to the cloud store
  */
class BlobStoreObjectStorageService(blobStoreContext: BlobStoreContext, objectContainerName: String)
  extends ObjectStorageService with Logging with InitializingBean with TaskBenchmarking {

  protected lazy val blobStore: BlobStore = blobStoreContext.getBlobStore

  private final val slicer = new BasePayloadSlicer

  override def afterPropertiesSet(): Unit = {
    // Create the container if it doesn't exist
    if (!blobStore.containerExists(objectContainerName)) blobStore.createContainerInLocation(null, objectContainerName)
  }

  override def keyExists(key: String): Future[Boolean] = Future { blobStore.blobExists(objectContainerName, key) }

  private def metadata(key: String) = key.maybeText.flatMap { k => Option(blobStore.blobMetadata(objectContainerName, k)) }

  override def push(key: String, in: ByteSource, metadata: ObjectStorageService.Metadata): Future[Unit] = Future {
    Assert.notNull(key, "Key must be defined")

    logger.debug(s"[$key] Pushing blob contentLength=${metadata.contentLength}")

    val blob = blobStore.blobBuilder(key)
      .payload(in)
      .contentDisposition(key)
      .contentType(metadata.contentType)
      .contentLength(metadata.contentLength)
      .userMetadata((metadata.userMetadata ++ metadata.fileHash.map { h => "shahex" -> h }.toMap).asJava)
      .build()

    // TAB-4144 Use large object support for anything over 50mb
    // TAB-4235 If you want this done in parallel, you have to do it yourself
    if (metadata.contentLength > 50 * 1024 * 1024) {
      val partSize =
        new MultipartUploadSlicingAlgorithm(blobStore.getMinimumMultipartPartSize, blobStore.getMaximumMultipartPartSize, blobStore.getMaximumNumberOfParts)
          .calculateChunkSize(metadata.contentLength)

      logger.debug(s"[$key] Using large object support, chunk size $partSize")

      val multipartUpload = blobStore.initiateMultipartUpload(objectContainerName, blob.getMetadata, PutOptions.NONE)

      val parts = slicer.slice(blob.getPayload, partSize).asScala.zipWithIndex.par.map { case (payload, index) =>
        logger.debug(s"[$key] Uploading multipart part ${index + 1}")
        blobStore.uploadMultipartPart(multipartUpload, index + 1, payload)
      }

      logger.debug(s"[$key] Completing multipart upload")
      blobStore.completeMultipartUpload(multipartUpload, parts.toList.asJava)
    } else {
      blobStore.putBlob(objectContainerName, blob, PutOptions.NONE)
    }
  }.map(_ => ())

  /**
    * Overridden in SwiftObjectStorageService to support multipart
    */
  protected def putBlob(blob: Blob, metadata: ObjectStorageService.Metadata): String = blobStore.putBlob(objectContainerName, blob)

  override def fetch(key: String): Future[RichByteSource] =
    Future.successful(new BlobBackedRichByteSource(blobStore, objectContainerName, key, metadata(key)))

  def delete(key: String): Future[Unit] = Future {
    blobStore.removeBlob(objectContainerName, key)
  }

  override def listKeys(): Future[Stream[String]] = Future {
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
    list(Option(firstResults.getNextMarker), firstResults.asScala.toStream).map(_.getName)
  }
}
