package uk.ac.warwick.tabula.services.objectstore

import java.io._

import com.google.common.io._
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.data.SHAFileHasherComponent
import uk.ac.warwick.tabula.helpers.DetectMimeType._
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global

import scala.concurrent.Future

class LegacyFilesystemObjectStorageService(attachmentDir: File, createMissingDirectories: Boolean = true)
  extends ObjectStorageService with SHAFileHasherComponent {

  private val idSplitSize = 2
  private val idSplitSizeCompat = 4 // for existing paths split by 4 chars

  /**
    * Retrieves a File object where you can store data under this ID. It doesn't check
    * whether the File already exists.
    */
  private def targetFile(key: String): File = {
    def partition(splitSize: Int): String = key.replace("-", "").grouped(splitSize).mkString("/")

    lazy val targetFile = new File(attachmentDir, partition(idSplitSize))
    lazy val targetFileCompat = new File(attachmentDir, partition(idSplitSizeCompat))

    // If no file found, check if it's stored under old 4-character path style
    if (!targetFile.exists() && targetFileCompat.exists()) targetFileCompat
    else targetFile
  }

  override def keyExists(key: String): Future[Boolean] = Future.successful(targetFile(key).exists())

  override def fetch(key: String): Future[RichByteSource] = Future.successful {
    val source = Files.asByteSource(targetFile(key))

    val metadata =
      if (source.isEmpty) None
      else Some(ObjectStorageService.Metadata(
        contentLength = source.size(),
        contentType = detectMimeType(source.openStream()).toString,
        fileHash = None
      ))

    RichByteSource.wrap(source, metadata)
  }

  override def push(key: String, in: ByteSource, /* ignored */ metadata: ObjectStorageService.Metadata): Future[Unit] = Future {
    val target = targetFile(key)
    val directory = target.getParentFile

    directory.mkdirs()
    if (!directory.exists) throw new IllegalStateException(s"Couldn't create directory to store file: $directory")

    FileCopyUtils.copy(in.openStream(), new FileOutputStream(target))
  }.map(_ => ())

  override def delete(key: String): Future[Unit] =
    Future.successful(targetFile(key).delete())

  override def listKeys(): Future[LazyList[String]] = Future {
    def toKey(file: File): String = {
      val stripped =
        file.getAbsolutePath.substring(attachmentDir.getAbsolutePath.length)
          .replace("/", "")

      // UUID format, 8-4-4-4-12
      s"${stripped.substring(0, 8)}-${stripped.substring(8, 12)}-${stripped.substring(12, 16)}-${stripped.substring(16, 20)}-${stripped.substring(20)}"
    }

    def files(base: File): LazyList[String] =
      if (base.isFile) LazyList(toKey(base))
      else base.listFiles().sortBy(_.getName).to(LazyList).flatMap(files)

    files(attachmentDir)
  }

  override def afterPropertiesSet(): Unit = {
    if (!attachmentDir.isDirectory) {
      if (createMissingDirectories) {
        attachmentDir.mkdirs()
      } else {
        throw new IllegalStateException("Attachment store '" + attachmentDir + "' must be an existing directory")
      }
    }
  }

}
