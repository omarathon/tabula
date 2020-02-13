package uk.ac.warwick.tabula.services.fileserver

import enumeratum.{Enum, EnumEntry}
import uk.ac.warwick.tabula.helpers.MimeTypeDetectionResult
import uk.ac.warwick.tabula.helpers.StringUtils._

sealed trait ContentDisposition extends EnumEntry {
  protected def headerValueKeyword(detectedMimeType: MimeTypeDetectionResult): String

  def headerValue(detectedMimeType: MimeTypeDetectionResult, suggestedFilename: Option[String]): String = {
    val builder = new StringBuilder
    builder.append(headerValueKeyword(detectedMimeType))

    suggestedFilename.filter(_.hasText).foreach { filename =>
      builder.append("; ")
      HttpHeaderParameterEncoding.encodeToBuilder("filename", filename, builder)
    }

    builder.toString
  }
}

object ContentDisposition extends Enum[ContentDisposition] {
  case object Attachment extends ContentDisposition {
    override protected def headerValueKeyword(detectedMimeType: MimeTypeDetectionResult): String = "attachment"
  }
  case object Inline extends ContentDisposition {
    override protected def headerValueKeyword(detectedMimeType: MimeTypeDetectionResult): String = "inline"
  }
  case object Default extends ContentDisposition {
    override protected def headerValueKeyword(detectedMimeType: MimeTypeDetectionResult): String =
      if (detectedMimeType.serveInline) "inline" else "attachment"
  }

  override def values: IndexedSeq[ContentDisposition] = findValues
}
