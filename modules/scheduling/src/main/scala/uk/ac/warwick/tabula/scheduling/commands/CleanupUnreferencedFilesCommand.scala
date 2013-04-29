package uk.ac.warwick.tabula.scheduling.commands
import java.io.File
import java.io.FileFilter
import java.io.FileWriter
import java.io.IOException
import org.joda.time.DateTime
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.util.core.spring.FileUtils
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.permissions._

/**
 * Go through each synced file on the filesystem and delete any file where there isn't
 * a corresponding FileAttachment in the database.
 *
 * This will ignore any files created in the last 6 hours, so we can ensure that if
 * there is any lag between syncing the database that the files aren't deleted before the
 * relevant database record can be synced across.
 */
class CleanupUnreferencedFilesCommand extends Command[Unit] with ReadOnly {
	import CleanupUnreferencedFilesCommand._
	import FunctionalFileFilter._

	PermissionCheck(Permissions.ReplicaSyncing)

	var fileDao = Wire.auto[FileDao]

	var dataDir = Wire[String]("${base.data.dir}")

	val startTime = DateTime.now
	val syncBuffer = startTime.minusHours(6)

	// Get a list of all IDs in the database for us to check against, stripping hyphens (since we do that when we check)
	lazy val ids = fileDao.getAllFileIds() map { _.replace("-", "") }

	lazy val lastCleanupJobDetailsFile = new File(new File(dataDir), LastCleanupJobDetailsFilename)

	override def applyInternal() {
		timed("Cleanup file references") { timer =>
			logger.debug("All ids size: " + ids.size)

			val (successful, deleted) = checkBucket(fileDao.attachmentDir)

			val logString = s"successfulFiles,$successful,deletedFiles,$deleted,timeTaken,${timer.getTotalTimeMillis},lastSuccessfulRun,${startTime.getMillis}" 
			logger.info(logString)

			try {
				FileCopyUtils.copy(logString, new FileWriter(lastCleanupJobDetailsFile))
			} catch {
				case e: IOException =>
					logger.error("Failed to update last cleanup job details: " + logString + " to file: " + lastCleanupJobDetailsFile, e)
			}
		}
	}

	private def checkBucket(directory: File, prefix: String = ""): (Int, Int) = {
		// Get all files in this directory created before the limit
		val files = directory.listFiles(filter { file =>
			file.isFile && file.lastModified < syncBuffer.getMillis
		})

		val deleted = (for (file <- files) yield {
			val id = prefix.concat(file.getName)
			if (ids contains id) {
				// The file exists both on the filesystem and in the database
				None
			} else if (fileDao.getFileByStrippedId(id).isDefined) {
				// This ID wasn't in our cached list, but it has since been created (or re-created) while cleanup was running
				None
			} else {
				logger.info("Deleting file as it no longer exists in the database: " + file)
				FileUtils.recursiveDelete(file)

				Some(file)
			}
		}).flatten

		val stats = (files.size - deleted.size, deleted.size)

		// Sub-buckets
		val buckets = directory.listFiles(filter { _.isDirectory }).toSeq

		val otherStats = for (bucket <- buckets)
			yield checkBucket(bucket, prefix.concat(bucket.getName))

		otherStats.foldLeft (stats) {(a, b) => (a._1 + b._1, a._2 + b._2)}
	}

	override def describe(d: Description) {}

}

object CleanupUnreferencedFilesCommand {
	val LastCleanupJobDetailsFilename = "last_replica_filesystem_cleanup_details"
}

class FunctionalFileFilter[A](fn: => (File => Boolean)) extends FileFilter {
	override def accept(file: File) = fn(file)
}

object FunctionalFileFilter {
	def filter(fn: => (File => Boolean)) = new FunctionalFileFilter(fn)
}