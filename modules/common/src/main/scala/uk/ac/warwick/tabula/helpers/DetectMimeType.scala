package uk.ac.warwick.tabula.helpers

import java.io.File
import org.apache.tika.detect.{DefaultDetector,Detector}
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MimeTypes
import org.apache.tika.io.TikaInputStream;

trait DetectMimeType {
	def detectMimeType(file: File): String = {
			val detector = new DefaultDetector(MimeTypes.getDefaultMimeTypes())
			var metadata = new Metadata
			
			var tikaIS: TikaInputStream = null
			
			try {
				tikaIS = TikaInputStream.get(file)
				detector.detect(tikaIS, metadata).toString
			} finally {
				if (tikaIS != null) tikaIS.close
			}
	}
}