package uk.ac.warwick.courses.commands
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.FileDao
import org.springframework.beans.factory.annotation.Configurable
import uk.ac.warwick.courses.data.Transactions._

@Configurable
class CleanupTemporaryFilesCommand extends Command[Unit] {

	@Autowired var dao: FileDao = _

	override def work = transactional() {
		dao.deleteOldTemporaryFiles
	}

	override def describe(d: Description) {}
}