package uk.ac.warwick.courses.services.fileserver
import java.io.InputStream
import java.io.File

trait RenderableFile {
	def inputStream:InputStream
	def filename:String
	def contentType:String
	def contentLength:Option[Long]
	
	/**
	 * Optional property - if present, fileserver may use it instead of
	 * inputStream to serve the content. (i.e. for SendFile support)
	 */
	def file:Option[File]
}