package uk.ac.warwick.tabula.scheduling.commands

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.FileDao
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.permissions._

class CleanupTemporaryFilesCommand extends Command[Unit] {
	
	PermissionCheck(Permission.ReplicaSyncing())

	var dao = Wire.auto[FileDao]

	override def applyInternal() = transactional() {
		dao.deleteOldTemporaryFiles
	}

	override def describe(d: Description) {}
}