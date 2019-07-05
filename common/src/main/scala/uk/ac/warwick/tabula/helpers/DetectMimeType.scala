package uk.ac.warwick.tabula.helpers

import java.io.InputStream

import org.apache.tika.detect.DefaultDetector
import org.apache.tika.io.TikaInputStream
import org.apache.tika.metadata.{HttpHeaders, Metadata, TikaMetadataKeys}
import org.apache.tika.mime.{MediaType, MimeTypes}
import uk.ac.warwick.tabula.helpers.Closeables._

object DetectMimeType {
  private val detector = new DefaultDetector(MimeTypes.getDefaultMimeTypes)

  private def detectMimeType(in: InputStream, metadata: Metadata): MediaType =
    closeThis(TikaInputStream.get(in)) { tikaIS =>
      detector.detect(tikaIS, metadata)
    }

  def detectMimeType(in: InputStream): MediaType = detectMimeType(in, new Metadata)

  def detectMimeType(in: InputStream, declaredFileName: String, delcaredContentType: String): MediaType = {
    val metadata = new Metadata
    metadata.set(TikaMetadataKeys.RESOURCE_NAME_KEY, declaredFileName)
    metadata.set(HttpHeaders.CONTENT_TYPE, delcaredContentType)
    detectMimeType(in, metadata)
  }
}