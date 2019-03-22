package uk.ac.warwick.tabula.services.fileserver

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.io.{IOUtils, TikaInputStream}
import org.apache.tika.metadata.{HttpHeaders, Metadata, TikaMetadataKeys}
import org.apache.tika.mime.{MediaType, MimeTypes}
import org.joda.time.DateTime
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
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

private[fileserver] object FileServer {
  private lazy val serveInlineMimeTypes: Set[MediaType] = Set(
    "text/plain",
    "application/pdf",
    "application/postscript",
    "image/*",
    "audio/*",
    "video/*",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/zip",
    "application/rtf",
    "text/calendar",
    "message/rfc822"
  ).map(MediaType.parse)

  def isServeInline(mediaType: MediaType): Boolean =
    serveInlineMimeTypes.exists { serveInline =>
      serveInline == mediaType || (serveInline.getSubtype == "*" && serveInline.getType == mediaType.getType)
    }
}

/**
  * Sets up the appropriate response headers and streams a renderable file
  *
  * We don't support using the content disposition header here as it's an unreliable way of setting the filename
  */
trait StreamsFiles {
  self: FeaturesComponent =>

  /**
    * Uses the magic bytes at the start of the file first, then the filename, then the supplied content type.
    */
  private val mimeTypeDetector = new DefaultDetector(MimeTypes.getDefaultMimeTypes)

  def stream(file: RenderableFile, fileName: Option[String] = None)(implicit request: HttpServletRequest, out: HttpServletResponse) {
    val mimeType: MediaType = file.contentType match {
      case "application/octet-stream" =>
        // We store files in the object store as application/octet-stream but we can just infer from the filename
        Option(file.byteSource).flatMap(bs => Option(bs.openStream())).map { inputStream =>
          val is = TikaInputStream.get(inputStream)
          try {
            val metadata = new Metadata
            metadata.set(TikaMetadataKeys.RESOURCE_NAME_KEY, file.filename)
            metadata.set(HttpHeaders.CONTENT_TYPE, file.contentType)

            mimeTypeDetector.detect(is, metadata)
          } finally {
            IOUtils.closeQuietly(is)

            // If we've detected a mime type, add XCTO so it's used
            out.setHeader("X-Content-Type-Options", "nosniff")
          }
        }.getOrElse(MediaType.parse(file.contentType))

      case _ => MediaType.parse(file.contentType)
    }

    out.setHeader("Content-Type", mimeType.toString)

    val builder = new StringBuilder
    builder.append(if (FileServer.isServeInline(mimeType)) "inline" else "attachment")

    file.suggestedFilename.orElse(fileName).orElse(Some(file.filename)).foreach { filename =>
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
