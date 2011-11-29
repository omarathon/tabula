package uk.ac.warwick.courses.helpers
import org.springframework.util.FileCopyUtils
import java.io.InputStreamReader
import java.io.InputStream

/**
 * Defines an implicit conversion to ReadableStream, allowing you to call these methods
 * as though they were methods of InputStream.
 */
trait StreamReading {
	class ReadableStream(val is:InputStream) {
		def readToString(encoding:String) = FileCopyUtils.copyToString(new InputStreamReader(is, encoding))
	}
	
	implicit def streamToReadable(is:InputStream) = new ReadableStream(is)
}