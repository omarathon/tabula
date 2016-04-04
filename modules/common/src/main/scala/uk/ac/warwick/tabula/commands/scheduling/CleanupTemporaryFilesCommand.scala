package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command, Description}
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions._

class CleanupTemporaryFilesCommand extends Command[Unit] {

	PermissionCheck(Permissions.ReplicaSyncing)

	var dao = Wire.auto[FileDao]

	override def applyInternal() = transactional() {
		dao.deleteOldTemporaryFiles
	}

	override def describe(d: Description) {}
}