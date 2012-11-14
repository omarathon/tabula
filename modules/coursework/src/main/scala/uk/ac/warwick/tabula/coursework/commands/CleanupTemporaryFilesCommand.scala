package uk.ac.warwick.tabula.coursework.commands
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.coursework.data.FileDao
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