package uk.ac.warwick.tabula.helpers

import java.io.File
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MimeTypes
import org.apache.tika.io.TikaInputStream
import uk.ac.warwick.tabula.helpers.Closeables._

object DetectMimeType {
	private val detector = new DefaultDetector(MimeTypes.getDefaultMimeTypes)

	def detectMimeType(file: File): String = {
		closeThis(TikaInputStream.get(file)) { tikaIS =>
			detector.detect(tikaIS, new Metadata).toString
		}
	}
}