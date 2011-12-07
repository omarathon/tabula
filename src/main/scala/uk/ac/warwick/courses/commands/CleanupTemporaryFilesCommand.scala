package uk.ac.warwick.courses.commands
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.FileDao
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.transaction.annotation.Transactional

@Configurable
class CleanupTemporaryFilesCommand extends Command[Unit] {
	
	@Autowired var dao:FileDao =_
	
	@Transactional
	override def apply = dao.deleteOldTemporaryFiles
	
	override def describe(d:Description)
}