package uk.ac.warwick.tabula.services.objectstore

import java.io.{File, FileInputStream, FileOutputStream, InputStream}

import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.data.SHAFileHasherComponent
import uk.ac.warwick.tabula.helpers.DetectMimeType._

class LegacyFilesystemObjectStorageService(attachmentDir: File, createMissingDirectories: Boolean = true)
	extends ObjectStorageService with SHAFileHasherComponent {

	if (!attachmentDir.isDirectory) {
		if (createMissingDirectories) {
			attachmentDir.mkdirs()
		} else {
			throw new IllegalStateException("Attachment store '" + attachmentDir + "' must be an existing directory")
		}
	}

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

	override def keyExists(key: String): Boolean = targetFile(key).exists()

	override def fetch(key: String): Option[InputStream] = targetFile(key) match {
		case f: File if f.exists() => Some(new FileInputStream(f))
		case _ => None
	}

	override def metadata(key: String): Option[ObjectStorageService.Metadata] = targetFile(key) match {
		case f: File if f.exists() => Some(ObjectStorageService.Metadata(
			contentLength = f.length(),
			contentType = detectMimeType(new FileInputStream(f)),
			fileHash = None
		))
		case _ => None
	}

	override def push(key: String, in: InputStream, /* ignored */ metadata: ObjectStorageService.Metadata): Unit = {
		val target = targetFile(key)
		val directory = target.getParentFile

		directory.mkdirs()
		if (!directory.exists) throw new IllegalStateException(s"Couldn't create directory to store file: $directory")

		FileCopyUtils.copy(in, new FileOutputStream(target))
	}

	override def delete(key: String): Unit = {
		targetFile(key).delete()
	}

	override def listKeys(): Stream[String] = {
		def toKey(file: File): String = {
			val stripped =
				file.getAbsolutePath.substring(attachmentDir.getAbsolutePath.length)
					.replace("/", "")

			// UUID format, 8-4-4-4-12
			s"${stripped.substring(0, 8)}-${stripped.substring(8, 12)}-${stripped.substring(12, 16)}-${stripped.substring(16, 20)}-${stripped.substring(20)}"
		}

		def files(base: File): Stream[String] =
			if (base.isFile) Stream(toKey(base))
			else
				base.listFiles().sortBy(_.getName)
					// This is a bit of a kludgy hack but it stops the object migration job transferring object store files to itself
					.filterNot(_.getName == "objectstore")
					.toStream.flatMap(files)

		files(attachmentDir)
	}

}
