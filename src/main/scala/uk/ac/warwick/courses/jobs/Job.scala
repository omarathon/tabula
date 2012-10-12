package uk.ac.warwick.courses.jobs

import org.springframework.beans.factory.annotation.Configurable
import org.springframework.transaction.annotation._
import org.springframework.stereotype.Service
import uk.ac.warwick.courses.services.jobs._
import org.springframework.stereotype.Component
import uk.ac.warwick.courses.helpers.Logging
import org.springframework.beans.factory.annotation.Autowired

/**
 * A Job is a task that is added to a queue and processed in the
 * background by {{uk.ac.warwick.courses.services.jobs.JobService}}.
 * They can post updates on their status and progress which can then
 * be viewed by anyone who knows the Job's ID.
 *
 * To make a new Job:
 *
 * - Extend Job
 * - Add @Component annotation
 * - Make sure to set succeeded=true at the end of your run method
 * - Define a companion object with a method to create a JobPrototype
 *     containing attributes that your Job can read out later.
 */
@Component
abstract class Job extends Logging {

	/** Identifies the specific Job in the database. */
	val identifier: String

	@Autowired var jobService: JobService = _

	/**
	 * Run the job. Job itself is stateless so
	 * JobInstance provides access to information about
	 * this specific job instance, and method to update
	 * status and progress.
	 */
	def run(implicit job: JobInstance): Unit

	protected def getProgress(implicit job: JobInstance) = job.progress
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	protected def updateProgress(percent: Int)(implicit job: JobInstance) {
		job.progress = percent
		jobService.update(job)
	}

	protected def getStatus(implicit job: JobInstance) = job.status
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	protected def updateStatus(status: String)(implicit job: JobInstance) {
		if (debugEnabled) logger.debug("Job:" + job.id + " - " + status)
		job.status = status
		jobService.update(job)
	}

	/** An exception you can throw when a Job is obsolete, e.g. it references an entity that no longer exists.
	 * JobService will catch this and remove the job. */
	protected def obsoleteJob = new ObsoleteJobException

}