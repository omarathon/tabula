package uk.ac.warwick.tabula.scheduling.commands

import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import org.springframework.util.FileCopyUtils
import java.io.File
import uk.ac.warwick.spring.Wire
import org.joda.time.DateTime
import java.io.FileReader
import uk.ac.warwick.tabula.data.FileDao
import java.io.IOException
import java.io.FileWriter
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.permissions._

/**
 * Job to go through each FileAttachment in the database and alert if there
 * isn't a corresponding file on the filesystem.
 * 
 * This will ignore any files that have been created since the last sync, if we
 * are a standby.
 */
class SanityCheckFilesystemCommand extends Command[Unit] with ReadOnly {
	import SyncReplicaFilesystemCommand._
	import SanityCheckFilesystemCommand._
	
	PermissionCheck(Permission.ReplicaSyncing())
	
	var fileSyncEnabled = Wire.property("${environment.standby}").toBoolean
	var dataDir = Wire[String]("${base.data.dir}")
	var fileDao = Wire.auto[FileDao]
	
	lazy val lastSanityCheckJobDetailsFile = new File(new File(dataDir), LastSanityCheckJobDetailsFilename)
	
	override def applyInternal() {
		val startTime = DateTime.now
		
		timed("Sanity check filesystem") { timer =>
			val allIds = fileDao.getAllFileIds(lastSyncDate)
			
			val (successful, unsuccessful) = ((for (id <- allIds) yield fileDao.getData(id) match {
				case Some(file) => (1, 0)
				case None =>
					// Check whether the file has since been cleaned up
					if (!fileDao.getFileById(id).isDefined) (0, 0)
					else {
						logger.error("*** File didn't exist for: " + id)
						(0, 1)
					}
			}).foldLeft((0, 0)) {(a, b) => (a._1 + b._1, a._2 + b._2)})
			
			val logString = "successfulFiles," + successful + ",failedFiles," + unsuccessful + ",timeTaken," + timer.getTotalTimeMillis + ",lastSuccessfulRun," + startTime.getMillis
			logger.info(logString)

			try {
				FileCopyUtils.copy(logString, new FileWriter(lastSanityCheckJobDetailsFile))
			} catch {
				case e: IOException =>
					logger.error("Failed to update last sanity check job details: " + logString + " to file: " + lastSanityCheckJobDetailsFile, e)
			}
		}
	}
	
	lazy val lastSyncDateFile = new File(new File(dataDir), LastSyncedDateFilename)
	
	private def lastSyncDate = {
		if (fileSyncEnabled && lastSyncDateFile.exists) {
			val line = FileCopyUtils.copyToString(new FileReader(lastSyncDateFile)).trim
			Some(new DateTime(line.toLong))
		} else None
	}
	
	override def describe(d: Description) {}

}

object SanityCheckFilesystemCommand {
	val LastSanityCheckJobDetailsFilename = "last_sanity_check_filesystem_details"
}