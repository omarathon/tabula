package uk.ac.warwick.tabula.helpers

import java.util

import com.google.common.io.ByteSource
import freemarker.template.utility.DeepUnwrap
import freemarker.template.{TemplateMethodModelEx, TemplateModel}
import org.apache.tika.io.{IOUtils, TikaInputStream}
import org.apache.tika.mime.MediaType
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.helpers.MimeTypeDetector._
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}

import scala.collection.JavaConverters._

case class MimeTypeDetectionResult(
  mediaType: MediaType,
  serveInline: Boolean,
  detected: Boolean = false
)

object MimeTypeDetector {

  def detect(file: RenderableFile): MimeTypeDetectionResult =
    detect(file.byteSource, file.filename, file.contentType)

  def detect(in: ByteSource, filename: String, declaredContentType: String): MimeTypeDetectionResult = {
    val (mediaType, detected) =
      declaredContentType match {
        case "application/octet-stream" =>
          // We store files in the object store as application/octet-stream but we can just infer from the filename
          Option(in).flatMap(bs => Option(bs.openStream())).map { inputStream =>
            val is = TikaInputStream.get(inputStream)
            try {
              (DetectMimeType.detectMimeType(is, filename, declaredContentType), true)
            } finally {
              IOUtils.closeQuietly(is)
            }
          }.getOrElse((MediaType.parse(declaredContentType), false))

        case _ => (MediaType.parse(declaredContentType), false)
      }

    MimeTypeDetectionResult(
      mediaType = mediaType,
      serveInline = isServeInline(mediaType),
      detected = detected
    )
  }

  private lazy val serveInlineMimeTypes: Set[MediaType] = Set(
    "text/plain",
    "application/pdf",
    "image/jpeg",
    "image/png",
    "image/gif",
    "image/tiff",
    "image/bmp",
    "image/pjpeg",
    "audio/*",
    "video/*"
  ).map(MediaType.parse)

  def isServeInline(mediaType: MediaType): Boolean =
    serveInlineMimeTypes.exists { serveInline =>
      serveInline == mediaType || (serveInline.getSubtype == "*" && serveInline.getType == mediaType.getType)
    }

}

class MimeTypeDetectorTag extends TemplateMethodModelEx {

  override def exec(arguments: util.List[_]): MimeTypeDetectionResult = {
    val args = arguments.asScala.map { model => DeepUnwrap.unwrap(model.asInstanceOf[TemplateModel]) }

    args match {
      case Seq(file: RenderableFile) =>
        detect(file)

      case Seq(a: FileAttachment) =>
        detect(new RenderableAttachment(a))

      case s => throw new IllegalArgumentException(s"Bad argument $s")
    }
  }

}