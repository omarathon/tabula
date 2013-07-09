package uk.ac.warwick.tabula.services.fileserver
import org.springframework.stereotype.Service
import java.io.InputStream
import javax.servlet.http.HttpServletResponse
import org.springframework.util.FileCopyUtils
import javax.servlet.http.HttpServletRequest
import org.joda.time.DateTime

@Service
class FileServer {
	def stream(file: RenderableFile)(implicit request: HttpServletRequest, out: HttpServletResponse) {
		val inStream = file.inputStream
		
		out.addHeader("Content-Type", file.contentType)
		
		file.contentLength.map { length =>
			out.addHeader("Content-Length", length.toString)
		}

		handleCaching(file, request, out)

		if (request.getMethod.toUpperCase != "HEAD")
			Option(inStream) map { FileCopyUtils.copy(_, out.getOutputStream) }
	}
	
	/**
	 * Serves a RenderableFile out to an HTTP response.
	 */
	def serve(file: RenderableFile)(implicit request: HttpServletRequest, out: HttpServletResponse) {
		/*
		 * There's no consistent standard for encoding in the optional
		 * "filename" attribute of Content-Disposition, so you should stick
		 * to the only reliable method of specifying the filename which
		 * is to put it as the last part of the URL path.
		 */
		out.addHeader("Content-Disposition", "attachment")
		
		stream(file)
	}

	// Very simplistic cache headers - needs turbocharging with TAB-929
	private def handleCaching(file: RenderableFile, request: HttpServletRequest, out: HttpServletResponse) {
		for (expires <- file.cachePolicy.expires) {
			out.setDateHeader("Expires", (DateTime.now plus expires).getMillis)
		}
	}
}