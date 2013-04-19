package uk.ac.warwick.tabula.scheduling.commands


import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.permissions._

class CleanupTemporaryFilesCommand extends Command[Unit] {
	
	PermissionCheck(Permissions.ReplicaSyncing)

	var dao = Wire[FileDao]

	override def applyInternal() = transactional() {
		dao.deleteOldTemporaryFiles
	}

	override def describe(d: Description) {}
}