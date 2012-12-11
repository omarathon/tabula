package uk.ac.warwick.tabula.services.fileserver
import org.springframework.stereotype.Service
import java.io.InputStream
import javax.servlet.http.HttpServletResponse
import org.springframework.util.FileCopyUtils
import javax.servlet.http.HttpServletRequest

@Service
class FileServer {
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
		val inStream = file.inputStream
		out.addHeader("Content-Disposition", "attachment")
		file.contentLength.map { length =>
			out.addHeader("Content-Length", length.toString)
		}
		
		if (request.getMethod.toUpperCase != "HEAD")
			FileCopyUtils.copy(inStream, out.getOutputStream)
	}
}