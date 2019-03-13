package uk.ac.warwick.tabula.services.fileserver

import java.util.{BitSet => JBitSet}

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.io.{IOUtils, TikaInputStream}
import org.apache.tika.metadata.{HttpHeaders, Metadata, TikaMetadataKeys}
import org.apache.tika.mime.{MediaType, MimeTypes}
import org.joda.time.DateTime
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
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
        Option(file.inputStream).map { inputStream =>
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

    file.suggestedFilename.orElse(fileName).foreach { filename =>
      builder.append("; ")
      HttpHeaderParameterEncoding.encodeToBuilder("filename", filename, builder)
    }

    out.setHeader("Content-Disposition", builder.toString)

    // Restrictive CSP, just enough for Firefox/Chrome's PDF viewer to work plus audio/video
    out.setHeader("Content-Security-Policy", "default-src 'none'; img-src 'self'; object-src 'self'; plugin-types application/pdf; style-src 'unsafe-inline'; media-src 'self'")

    handleCaching(file, request, out)

    if (request.getMethod.toUpperCase != "HEAD") {
      file.contentLength.foreach { length => out.setHeader("Content-Length", length.toString) }
      Option(file.inputStream).foreach { FileCopyUtils.copy(_, out.getOutputStream) }
    } else {
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

/**
  * Grokked from play.core.utils.HttpHeaderParameterEncoding
  *
  * Support for rending HTTP header parameters according to RFC5987.
  */
private[fileserver] object HttpHeaderParameterEncoding {
  private def charSeqToBitSet(chars: Seq[Char]): JBitSet = {
    val ints: Seq[Int] = chars.map(_.toInt)
    val max = ints.fold(0)(Math.max)
    assert(max <= 256) // We should only be dealing with 7 or 8 bit chars
    val bitSet = new JBitSet(max)
    ints.foreach(bitSet.set)
    bitSet
  }

  // From https://tools.ietf.org/html/rfc2616#section-2.2
  //
  //   separators     = "(" | ")" | "<" | ">" | "@"
  //                  | "," | ";" | ":" | "\" | <">
  //                  | "/" | "[" | "]" | "?" | "="
  //                  | "{" | "}" | SP | HT
  //
  // Rich: We exclude <">, "\" since they can be used for quoting/escaping and HT since it is
  // rarely used and seems like it should be escaped.
  private val Separators: Seq[Char] = Seq('(', ')', '<', '>', '@', ',', ';', ':', '/', '[', ']', '?', '=', '{', '}', ' ')

  private val AlphaNum: Seq[Char] = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')

  // From https://tools.ietf.org/html/rfc5987#section-3.2.1:
  //
  // attr-char     = ALPHA / DIGIT
  // / "!" / "#" / "$" / "&" / "+" / "-" / "."
  // / "^" / "_" / "`" / "|" / "~"
  // ; token except ( "*" / "'" / "%" )
  private val AttrCharPunctuation: Seq[Char] = Seq('!', '#', '$', '&', '+', '-', '.', '^', '_', '`', '|', '~')

  /**
    * A subset of the 'qdtext' defined in https://tools.ietf.org/html/rfc2616#section-2.2. These are the
    * characters which can be inside a 'quoted-string' parameter value. These should form a
    * superset of the [[AttrChar]] set defined below. We exclude some characters which are technically
    * valid, but might be problematic, e.g. "\" and "%" could be treated as escape characters by some
    * clients. We can be conservative because we can express these characters clearly as an extended
    * parameter.
    */
  private val PartialQuotedText: JBitSet = charSeqToBitSet(
    AlphaNum ++ AttrCharPunctuation ++
      // we include 'separators' plus some chars excluded from 'attr-char'
      Separators ++ Seq('*', '\''))

  /**
    * The 'attr-char' values defined in https://tools.ietf.org/html/rfc5987#section-3.2.1. Should be a
    * subset of [[PartialQuotedText]] defined above.
    */
  private val AttrChar: JBitSet = charSeqToBitSet(AlphaNum ++ AttrCharPunctuation)

  private val PlaceholderChar: Char = '?'

  /**
    * Render a parameter name and value, handling character set issues as
    * recommended in RFC5987.
    *
    * Examples:
    * [[
    * render("filename", "foo.txt") ==> "filename=foo.txt"
    * render("filename", "naïve.txt") ==> "filename=na_ve.txt; filename*=utf8''na%C3%AFve.txt"
    * ]]
    */
  def encodeToBuilder(name: String, value: String, builder: StringBuilder): Unit = {
    // This flag gets set if we encounter extended characters when rendering the
    // regular parameter value.
    var hasExtendedChars = false

    // Render ASCII parameter
    // E.g. naïve.txt --> "filename=na_ve.txt"

    builder.append(name)
    builder.append("=\"")

    // Iterate over code points here, because we only want one
    // ASCII character or placeholder per logical character. If
    // we use the value's encoded bytes or chars then we might
    // end up with multiple placeholders per logical character.
    value.codePoints().forEach { codePoint =>
      // We could support a wider range of characters here by using
      // the 'token' or 'quoted printable' encoding, however it's
      // simpler to use the subset of characters that is also valid
      // for extended attributes.
      if (codePoint >= 0 && codePoint <= 255 && PartialQuotedText.get(codePoint)) {
        builder.append(codePoint.toChar)
      } else {
        // Set flag because we need to render an extended parameter.
        hasExtendedChars = true
        // Render a placeholder instead of the unsupported character.
        builder.append(PlaceholderChar)
      }
    }

    builder.append('"')

    // Optionally render extended, UTF-8 encoded parameter
    // E.g. naïve.txt --> "; filename*=utf8''na%C3%AFve.txt"
    //
    // Renders both regular and extended parameters, as suggested by:
    // - https://tools.ietf.org/html/rfc5987#section-4.2
    // - https://tools.ietf.org/html/rfc6266#section-4.3 (for Content-Disposition filename parameter)

    if (hasExtendedChars) {
      def hexDigit(x: Int): Char = (if (x < 10) x + '0' else x - 10 + 'a').toChar

      // From https://tools.ietf.org/html/rfc5987#section-3.2.1:
      //
      // Producers MUST use either the "UTF-8" ([RFC3629]) or the "ISO-8859-1"
      // ([ISO-8859-1]) character set.  Extension character sets (mime-

      val CharacterSetName = "utf-8"

      builder.append("; ")
      builder.append(name)

      builder.append("*=")
      builder.append(CharacterSetName)
      builder.append("''")

      // From https://tools.ietf.org/html/rfc5987#section-3.2.1:
      //
      // Inside the value part, characters not contained in attr-char are
      // encoded into an octet sequence using the specified character set.
      // That octet sequence is then percent-encoded as specified in Section
      // 2.1 of [RFC3986].

      val bytes = value.getBytes(CharacterSetName)
      for (b <- bytes) {
        if (AttrChar.get(b & 0xFF)) {
          builder.append(b.toChar)
        } else {
          builder.append('%')
          builder.append(hexDigit((b >> 4) & 0xF))
          builder.append(hexDigit(b & 0xF))
        }
      }
    }
  }
}