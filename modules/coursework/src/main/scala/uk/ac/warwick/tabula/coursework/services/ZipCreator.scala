package uk.ac.warwick.tabula.coursework.services

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import scala.annotation.implicitNotFound
import scala.collection.mutable.ListBuffer
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.UnicodeExtraFieldPolicy
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import uk.ac.warwick.tabula.helpers.Logging
import org.hibernate.id.GUIDGenerator
import java.util.UUID

/**
 * An item in a Zip file. Can be a file or a folder.
 */
trait ZipItem {
	val name: String
}
case class ZipFileItem(val name: String, val input: InputStream) extends ZipItem
case class ZipFolderItem(val name: String, startItems: Seq[ZipItem] = Nil) extends ZipItem {
	var items: ListBuffer[ZipItem] = ListBuffer()
	items.appendAll(startItems)
}

/**
 * Mixin trait for creating zip files based on a list of ZipItems.
 * With ZipItem it's easier to build hierarchical folders of items, and you
 * can reuse methods that create items to nest them further into folders,
 * which is difficult to do directly with ZipOutputStream.
 *
 * Requires a "zipDir" property to be set, relating to the base directory
 * where resulting files should be stored.
 *
 * If an error occurs while writing the zip, the target file is deleted
 * (this is because our zip files are generally created and left in place to
 * be re-used later, so it's better to delete and try recreating later
 * than to keep serving half a corrupt file).
 */
trait ZipCreator extends Logging {

	def zipDir: File

	/**
	 * General method for building a zip out of a list of ZipItems.
	 * A ZipItem can be a ZipFolderItem for defining hierarchies of files.
	 *
	 * name will be a path underneath the zipDir root, e.g.
	 * "feedback/ab/cd/ef/123". A zip extension will be added.
	 *
	 * If a zip of the given name already exists, it returns that file
	 * instead of regenerating the file. The app has to remember to call
	 * invalidateZip whenever the contents of the zip would change, otherwise
	 * it becomes stale.
	 */
	def getZip(name: String, items: Seq[ZipItem]) = {
		val file = fileForName(name)
		if (!file.exists) writeToFile(file, items)
		file
	}

	/**
	 * Create a new Zip with a randomly generated name.
	 */
	def createUnnamedZip(items: Seq[ZipItem]) = {
		val file = unusedFile
		writeToFile(file, items)
		file
	}

	private def writeToFile(file: File, items: Seq[ZipItem]) = {
		file.getParentFile.mkdirs
		openZipStream(file) { (zip) =>
			zip.setLevel(9)
			// HFC-70 Windows compatible, but fixes filenames in good apps like 7-zip 
			zip.setCreateUnicodeExtraFields(UnicodeExtraFieldPolicy.NOT_ENCODEABLE)
			writeItems(items, zip)
		}
	}

	/** Try 100 times to get an unused filename */
	private def unusedFile = Stream.range(1, 100)
		.map(_ => fileForName(randomUUID))
		.find(!_.exists)
		.getOrElse(throw new IllegalStateException("Couldn't find unique filename"))

	private def randomUUID = UUID.randomUUID.toString().replace("-", "")

	/**
	 * Invalidates a previously created zip, by deleting its file.
	 *
	 * @param name The name as passed to getZip when creating the file.
	 */
	def invalidate(name: String) = fileForName(name).delete();

	private def fileForName(name: String) = new File(zipDir, name + ".zip")

	private def writeItems(items: Seq[ZipItem], zip: ZipArchiveOutputStream) {
		def writeFolder(basePath: String, items: Seq[ZipItem]) {
			for (item <- items) item match {
				case file: ZipFileItem => {
					zip.putArchiveEntry(new ZipArchiveEntry(basePath + file.name))
					copy(file.input, zip)
					zip.closeArchiveEntry()
				}
				case folder: ZipFolderItem => writeFolder(basePath + folder.name + "/", folder.items)
			}
		}
		writeFolder("", items)
	}

	/**
	 * Opens a zip output stream from this file, and runs the given function.
	 * The output stream is always closed, and if anything bad happens the file
	 * is deleted.
	 */
	private def openZipStream(file: File)(fn: (ZipArchiveOutputStream) => Unit) {
		var zip: ZipArchiveOutputStream = null;
		try {
			zip = new ZipArchiveOutputStream(file)
			fn(zip)
		} catch {
			case e: Exception => {
				logger.error("Exception creating zip file, deleting %s" format file)
				file.delete
				throw e
			}
		} finally {
			if (zip != null) zip.close
		}
	}

	// copies from is to os, but doesn't close os
	private def copy(is: InputStream, os: OutputStream) {
		try {
			// not sure how to create a byte[] directly, this seems reasonable.
			val buffer = ByteBuffer.allocate(4096).array
			// "continually" creates an endless iterator, "takeWhile" gives it an end
			val iterator = Iterator.continually { is.read(buffer) }.takeWhile { _ != -1 }
			for (read <- iterator) {
				os.write(buffer, 0, read)
			}
		} finally {
			is.close
		}
	}

}