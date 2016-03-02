package uk.ac.warwick.tabula.services.objectstore.multipart

/**
	* Copied from JClouds 2.0.0
	*
	* @see org.jclouds.blobstore.strategy.internal.MultipartUploadSlicingAlgorithm
	* @see https://github.com/jclouds/jclouds/blob/master/blobstore/src/main/java/org/jclouds/blobstore/strategy/internal/MultipartUploadSlicingAlgorithm.java
	*/
object MultipartUploadSlicingAlgorithm {
	val DEFAULT_PART_SIZE: Long = 33554432 // 32MB
	val DEFAULT_MAGNITUDE_BASE = 100

	val MINIMUM_MULTIPART_PART_SIZE = 1024 * 1024 + 1 // 1MB + 1 byte
	val MAXIMUM_MULTIPART_PART_SIZE = 5 * 1024 * 1024 * 1024 // 5GB
	val MAXIMUM_PARTS = Integer.MAX_VALUE

	case class Result(chunkSize: Long, parts: Int, remaining: Long)

	def calculateChunkSize(length: Long): Result = {
		var unitPartSize = DEFAULT_PART_SIZE // first try with default part size
		var parts = (length / unitPartSize).toInt

		var partSize = unitPartSize
		val magnitude = parts / DEFAULT_MAGNITUDE_BASE

		if (magnitude > 0) {
			partSize = magnitude * unitPartSize

			if (partSize > MAXIMUM_MULTIPART_PART_SIZE) {
				partSize = MAXIMUM_MULTIPART_PART_SIZE
				unitPartSize = MAXIMUM_MULTIPART_PART_SIZE
			}
			parts = (length / partSize).toInt

			if (parts * partSize < length) {
				partSize = (magnitude + 1) * unitPartSize
				if (partSize > MAXIMUM_MULTIPART_PART_SIZE) {
					partSize = MAXIMUM_MULTIPART_PART_SIZE
					unitPartSize = MAXIMUM_MULTIPART_PART_SIZE
				}
				parts = (length / partSize).toInt
			}
		}

		if (partSize > MAXIMUM_MULTIPART_PART_SIZE) {
			partSize = MAXIMUM_MULTIPART_PART_SIZE
			unitPartSize = MAXIMUM_MULTIPART_PART_SIZE
			parts = (length / unitPartSize).toInt
		}

		if (parts > MAXIMUM_MULTIPART_PART_SIZE) { // if splits in too many parts or cannot be split
			unitPartSize = MINIMUM_MULTIPART_PART_SIZE // take the minimum part size
			parts = (length / unitPartSize).toInt
		}

		if (parts > MAXIMUM_MULTIPART_PART_SIZE) { // if still splits in too many parts
			parts = MAXIMUM_PARTS - 1 // limit them. do not care about not covering
		}

		val remainder = length % unitPartSize
		if (remainder == 0 && parts > 0) {
			parts -= 1
		}

		Result(chunkSize = partSize, parts = parts, remaining = length - partSize * parts)
	}
}