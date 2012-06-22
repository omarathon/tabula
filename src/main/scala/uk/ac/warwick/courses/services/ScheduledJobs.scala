package uk.ac.warwick.courses.services

import uk.ac.warwick.courses
import org.{springframework => spring}

import spring.scheduling.annotation.Scheduled
import spring.stereotype.Service
import spring.beans.factory.annotation.Autowired
import courses.data.FileDao
import courses.commands.CleanupTemporaryFilesCommand
import courses.commands.imports.ImportModulesCommand
import courses.system.exceptions._
import courses.services.jobs.JobService
import reflect.BeanProperty
import collection.mutable

/**
 * The scheduled jobs don't particularly have to all be in one class,
 * but I decided it's better to have them all together than it is to have
 * the scheduled method in a related class (since it does so little) - nick
 */
@Service
class ScheduledJobs extends SchedulingConcurrency {
	
	@Autowired @BeanProperty 
	var exceptionResolver:ExceptionResolver =_
	
	@Autowired @BeanProperty
	var indexingService:AuditEventIndexService =_
	
	@Autowired @BeanProperty
	var jobService:JobService =_
	
	
	
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
	
	@Scheduled(cron="*/10 * * * * *") // every 10 seconds
	def jobs:Unit = nonconcurrent("jobs") { 
		exceptionResolver.reportExceptions { jobService.run }
	}
}

/**
 * Provides `nonconcurrent` which will only run the given
 * code if it's not currently running (as determined by the
 * ID string also passed). Used to avoid running a task if it's
 * still running already. 
 */
trait SchedulingConcurrency {
	var running = collection.mutable.Set[String]() 
	def nonconcurrent[T] (id:String) (f: =>T) =
		if (running.add(id)) { 
			try f
			finally running.remove(id)
		}
}