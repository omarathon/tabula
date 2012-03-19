package uk.ac.warwick.courses.services
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.data.FileDao
import uk.ac.warwick.courses.commands.CleanupTemporaryFilesCommand
import uk.ac.warwick.courses.commands.imports.ImportModulesCommand
import uk.ac.warwick.courses.system.exceptions.ExceptionHandler
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.system.exceptions.ExceptionResolver

/**
 * The scheduled jobs don't particularly have to all be in one class,
 * but I decided it's better to have them all together than it is to have
 * the scheduled method in a related class (since it does so little) - nick
 */
@Service
class ScheduledJobs {
	
	@Autowired @BeanProperty 
	var exceptionResolver:ExceptionResolver =_
	
	@Autowired @BeanProperty
	var indexingService:AuditEventIndexService =_
	
	/*
	 * Don't think @Transactional works on these methods so it should be put
	 * on the method that we call through to.
	 */
	  
    @Scheduled(cron="0 0 7,14 * * *")
    def importData:Unit = exceptionResolver.reportExceptions { 
    	new ImportModulesCommand().apply()
	}
	
	@Scheduled(cron="0 0 2 * * *") // 2am
	def cleanupTemporaryFiles:Unit = exceptionResolver.reportExceptions {  
		new CleanupTemporaryFilesCommand().apply()
	}
	
	@Scheduled(cron="0 */5 * * * *") // every 5 minutes
	def indexAuditEvents:Unit = exceptionResolver.reportExceptions { indexingService.index }
	
}