package uk.ac.warwick.tabula.services.fileserver

import com.google.common.io.ByteSource

trait RenderableFile {
	def byteSource: ByteSource
	def filename: String
	def contentType: String
	def contentLength: Option[Long]
	def suggestedFilename: Option[String] = None

	def cachePolicy = CachePolicy()
}