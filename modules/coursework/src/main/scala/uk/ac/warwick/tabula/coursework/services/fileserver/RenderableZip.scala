package uk.ac.warwick.tabula.coursework.services.fileserver

import java.io.File
import java.io.InputStream
import java.io.FileInputStream

class RenderableZip(zip: File) extends RenderableFile {
	def inputStream: InputStream = new FileInputStream(zip)
	def filename: String = zip.getName
	def contentType: String = "application/zip"
	def file: Option[File] = Some(zip)
	def contentLength = Some(zip.length)
}