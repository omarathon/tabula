package uk.ac.warwick.tabula.services.objectstore.multipart

import org.jclouds.blobstore.domain.BlobMetadata
import org.jclouds.blobstore.options.PutOptions
import uk.ac.warwick.tabula.services.objectstore.ObjectStorageService

case class MultipartUpload(key: String, id: String, blobMetadata: BlobMetadata, putOptions: PutOptions = PutOptions.NONE)

object MultipartUpload {
	def apply(blobMetadata: BlobMetadata, metadata: ObjectStorageService.Metadata, partSize: Long): MultipartUpload = MultipartUpload(
		key = blobMetadata.getName,
		id = "%s/slo/%.6f/%s/%s".format(blobMetadata.getName, System.currentTimeMillis() / 1000.0, metadata.contentLength, partSize),
		blobMetadata = blobMetadata
	)
}

case class MultipartPart(name: String, number: Int, size: Long, eTag: String)
