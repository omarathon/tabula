package uk.ac.warwick.tabula.commands

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.FileDao
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.spring.Wire

class CleanupTemporaryFilesCommand extends Command[Unit] {

	var dao = Wire.auto[FileDao]

	override def work = transactional() {
		dao.deleteOldTemporaryFiles
	}

	override def describe(d: Description) {}
}