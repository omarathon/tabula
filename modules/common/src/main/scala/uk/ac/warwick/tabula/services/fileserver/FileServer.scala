package uk.ac.warwick.tabula.services.fileserver
import org.springframework.stereotype.Service
import java.io.InputStream
import javax.servlet.http.HttpServletResponse
import org.springframework.util.FileCopyUtils
import javax.servlet.http.HttpServletRequest
import org.joda.time.DateTime
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.spring.Wire

@Service
class FileServer {
	var features = Wire[Features]

	def stream(file: RenderableFile)(implicit request: HttpServletRequest, out: HttpServletResponse) {
		val inStream = file.inputStream
		
		out.addHeader("Content-Type", file.contentType)

		handleCaching(file, request, out)

		if (request.getMethod.toUpperCase != "HEAD") {
			if (file.file.isDefined && features.xSendfile) {
				out.addHeader("X-Sendfile", file.file.get.getAbsolutePath)
			} else {
				file.contentLength.foreach { length => out.addHeader("Content-Length", length.toString) }

				Option(inStream).foreach { FileCopyUtils.copy(_, out.getOutputStream) }
			}
		} else {
			file.contentLength.foreach { length => out.addHeader("Content-Length", length.toString) }
		}
	}
	
	/**
	 * Serves a RenderableFile out to an HTTP response.
	 */
	def serve(file: RenderableFile, fileName: Option[String] = None)(implicit request: HttpServletRequest, out: HttpServletResponse) {
		/*
		 * There's no consistent standard for encoding in the optional
		 * "filename" attribute of Content-Disposition, so you should stick
		 * to the only reliable method of specifying the filename which
		 * is to put it as the last part of the URL path.
		 */
		val dispositionHeader = fileName match {
			case Some(fileName) => "attachment;filename=\"" + fileName + "\""
			case _ => "attachment"
		}

		out.addHeader("Content-Disposition", dispositionHeader)
		
		stream(file)
	}

	// Very simplistic cache headers - needs turbocharging with TAB-929
	private def handleCaching(file: RenderableFile, request: HttpServletRequest, out: HttpServletResponse) {
		for (expires <- file.cachePolicy.expires) {
			out.setDateHeader("Expires", (DateTime.now plus expires).getMillis)
		}
	}
}