package uk.ac.warwick.tabula.services.fileserver

import java.io.File

import com.google.common.io.Files
import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.{AutowiringFileDaoComponent, FileDaoComponent}
import uk.ac.warwick.tabula.helpers.JodaConverters._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.util.files.hash.HashString
import uk.ac.warwick.util.files.imageresize.{ImageResizer, JAIImageResizer}

import scala.jdk.CollectionConverters._
import scala.util.Using

trait UploadedImageProcessor {
  def fixOrientation(file: UploadedFile): Unit
}

abstract class AbstractUploadedImageProcessor extends UploadedImageProcessor with Logging {
  self: ImageResizerComponent
    with FileDaoComponent =>

  override def fixOrientation(file: UploadedFile): Unit = transactional() {
    // For jpegs with EXIF orientation, transform and remove the EXIF data
    file.attached.asScala
      .filter { a =>
        a.temporary && a.mimeType == "image/jpeg"
      }
      .map { a =>
        (a, Using.resource(a.asByteSource.openStream()) { is =>
          JAIImageResizer.getOrientation(is, ImageResizer.FileType.jpg)
        })
      }
      .filter { case (_, orientation) => orientation != ImageResizer.Orientation.Normal }
      .foreach { case (a, orientation) =>
        logger.info(s"Fixing the orientation of $a ($orientation)")

        // Stream the re-oriented image to the file system
        val file = File.createTempFile(a.name, ".tmp")
        val outputSink = Files.asByteSink(file)

        try {
          Using(outputSink.openStream()) { out =>
            imageResizer.renderResized(
              a.asByteSource,
              new HashString(a.hash),
              a.dateUploaded.asJava,
              out,
              0,
              0,
              ImageResizer.FileType.jpg
            )
          }

          a.uploadedData = Files.asByteSource(file)
          a.overwrite = true
          a.dateUploaded = DateTime.now
          fileDao.saveTemporary(a)
        } finally {
          if (!file.delete()) file.deleteOnExit()
        }
      }
  }
}

@Service("uploadedImageProcessor")
class AutowiredUploadedImageProcessor
  extends AbstractUploadedImageProcessor
    with AutowiringImageResizerComponent
    with AutowiringFileDaoComponent

trait UploadedImageProcessorComponent {
  def uploadedImageProcessor: UploadedImageProcessor
}

trait AutowiringUploadedImageProcessorComponent extends UploadedImageProcessorComponent {
  var uploadedImageProcessor: UploadedImageProcessor = Wire[UploadedImageProcessor]
}

trait ImageResizerComponent {
  def imageResizer: ImageResizer
}

trait AutowiringImageResizerComponent extends ImageResizerComponent {
  var imageResizer: ImageResizer = Wire[ImageResizer]
}
