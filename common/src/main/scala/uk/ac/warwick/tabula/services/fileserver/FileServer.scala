package uk.ac.warwick.tabula.services.fileserver

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.mime.{MediaType, MimeTypes}
import org.joda.time.DateTime
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.helpers.MimeTypeDetector
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}

@Service
class FileServer extends StreamsFiles with AutowiringFeaturesComponent {
  /**
    * Serves a RenderableFile out to an HTTP response.
    */
  def serve(file: RenderableFile, fileName: String)(implicit request: HttpServletRequest, out: HttpServletResponse): Unit =
    stream(file, Some(fileName))
}

/**
  * Sets up the appropriate response headers and streams a renderable file
  *
  * We don't support using the content disposition header here as it's an unreliable way of setting the filename
  */
trait StreamsFiles {
  self: FeaturesComponent =>

  def stream(file: RenderableFile, fileName: Option[String] = None)(implicit request: HttpServletRequest, out: HttpServletResponse) {
    val detectedMimeType = MimeTypeDetector.detect(file)
    if (detectedMimeType.detected) {
      // If we've detected a mime type, add XCTO so it's used
      out.setHeader("X-Content-Type-Options", "nosniff")
    }

    val mimeType: MediaType = detectedMimeType.mediaType
    out.setHeader("Content-Type", mimeType.toString)

    val builder = new StringBuilder
    builder.append(if (detectedMimeType.serveInline) "inline" else "attachment")

    file.suggestedFilename.orElse(fileName).orElse(Option(file.filename).filter(_.hasText)).foreach { filename =>
      builder.append("; ")
      HttpHeaderParameterEncoding.encodeToBuilder("filename", filename, builder)
    }

    out.setHeader("Content-Disposition", builder.toString)

    // Restrictive CSP, just enough for viewing inline
    out.setHeader("Content-Security-Policy",
      "default-src 'none'; " +
        "img-src 'self' data:; " + // View images inline; allow data: for Safari media player
        "object-src 'self'; " + // Allow plugins to load for the current context
        "plugin-types application/pdf; " + // Only allow the PDF plugin
        "style-src 'unsafe-inline'; " + // PDF viewer Chrome?
        "media-src 'self'" // Needed to load the audio/video
    )

    handleCaching(file, request, out)

    if (request.getMethod.toUpperCase != "HEAD") {
      (file.contentLength, Option(file.byteSource)) match {
        case (Some(totalLength), Some(byteSource)) =>
          // Inform the client that we accept byte-range requests
          out.setHeader("Accept-Ranges", "bytes")

          RangeSet(Some(totalLength), Option(request.getHeader("Range")).filter(_.hasText)) match {
            case rangeSet: SatisfiableRangeSet =>
              val firstRange = rangeSet.first
              val byteRange = firstRange.byteRange

              out.setStatus(HttpStatus.PARTIAL_CONTENT.value())
              out.setHeader("Content-Range", rangeSet.toString)
              out.setHeader("Content-Length", firstRange.length.toString)

              byteSource.slice(byteRange.start, byteRange.length + 1).copyTo(out.getOutputStream)

            case rangeSet: UnsatisfiableRangeSet =>
              out.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value())
              out.setHeader("Content-Range", rangeSet.toString)

            case _: NoHeaderRangeSet =>
              // Serve the whole file
              out.setHeader("Content-Length", totalLength.toString)
              byteSource.copyTo(out.getOutputStream)
          }

        case _ =>
        // Do nothing, it's gone
      }
    } else {
      // Inform the client that we accept byte-range requests
      out.setHeader("Accept-Ranges", "bytes")

      file.contentLength.foreach { length => out.setHeader("Content-Length", length.toString) }
    }
  }

  // Very simplistic cache headers - needs turbocharging with TAB-929
  private def handleCaching(file: RenderableFile, request: HttpServletRequest, out: HttpServletResponse) {
    out.setHeader("Cache-Control", "private")
    for (expires <- file.cachePolicy.expires) {
      out.setDateHeader("Expires", (DateTime.now plus expires).getMillis)
    }
  }
}
