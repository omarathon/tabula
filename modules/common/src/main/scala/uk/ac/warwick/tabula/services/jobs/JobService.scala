package uk.ac.warwick.tabula.services.jobs

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.events.JobNotificationHandling
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.jobs.{FailedJobException, Job, JobPrototype, ObsoleteJobException}
import uk.ac.warwick.userlookup.User

trait JobServiceComponent {
	def jobService: JobService
}

trait AutowiringJobServiceComponent extends JobServiceComponent {
	var jobService = Wire[JobService]
}

@Service
class JobService extends HasJobDao with Logging with JobNotificationHandling {
	import uk.ac.warwick.tabula.data.Transactions._

	// How many jobs to load and run each time
	val RunBatchSize = 10

	/** Spring should wire in all beans that extend Job */
	@Autowired var jobs: Array[Job] = Array()

	def run() {
		val runningJobs = jobDao.listRunningJobs
		if (runningJobs.size < RunBatchSize) {
			jobDao
				.findOutstandingInstances(RunBatchSize - runningJobs.size)
				.filterNot(prospectiveJob => runningJobs.exists(runningJob =>
					runningJob.jobType == prospectiveJob.jobType && runningJob.json == prospectiveJob.json
				))
				.par
				.foreach(processInstance)
		}
	}

	def getInstance(id: String) = jobDao.getById(id)

	def processInstance(instance: JobInstance) {
		findJob(instance.jobType) match {
			case Some(job) => processInstance(instance, job)
			case _ => logger.warn("Couldn't find a job matching for this instance: " + instance)
		}
	}

	def processInstance(instance: JobInstance, job: Job) {
		start(instance)
		run(instance, job)
	}

	def kill(instance: JobInstance) {
		/**
		 * TODO no handle on thread to actually kill it if it's running
		 * right now.
		 */

		// Don't fail if the job is finished
		if (!instance.finished) {
			transactional() {
				instance.succeeded = false
				instance.finished = true
				instance.status = "Killed"
				jobDao.update(instance)
			}
		}
	}

	def unfinishedInstances = jobDao.unfinishedInstances
	def listRecent(start: Int, count: Int) = jobDao.listRecent(start, count)

	def update(instance: JobInstance) = jobDao.update(instance)

	def findJob(identifier: String) =
		jobs.find(identifier == _.identifier)

	def add(user: Option[CurrentUser], prototype: JobPrototype): JobInstance = {
		add(user.map(_.realId), user.map(_.apparentId), prototype)
	}

	def add(user: User, prototype: JobPrototype): JobInstance = {
		add(Some(user.getUserId), Some(user.getUserId), prototype)
	}

	private def add(realUserId: Option[String], apparentUserId: Option[String], prototype: JobPrototype) = {
		if (findJob(prototype.identifier).isEmpty) {
			throw new IllegalArgumentException("No Job found to handle '%s'" format prototype.identifier)
		}

		val instance = JobInstanceImpl.fromPrototype(prototype)
		realUserId.foreach(id => instance.realUser = id)
		apparentUserId.foreach(id => instance.apparentUser = id)

		// Do we already have an outstanding job with these details? Outstanding; just return that.
		jobDao.findOutstandingInstance(instance) getOrElse {
			jobDao.saveJob(instance)

			instance
		}
	}

	def run(instance: JobInstance, job: Job) {
			try job.run(instance)
			catch {
				case old: ObsoleteJobException =>
					logger.info("Job " + instance.id + " obsolete")
					fail(instance)
				case failed: FailedJobException =>
					logger.info("Job " + instance.id + " failed: " + failed.status)
					instance.status = failed.status
					fail(instance)
				case e: Throwable =>
					logger.info("Job " + instance.id + " failed", e)
					instance.status = "Sorry, there was an error: " + e.getMessage
					fail(instance)
			}
		transactional() {
			notify(instance, job) // send any notifications generated by the job
		}
			finish(instance)
	}

	private def start(instance: JobInstance) {
		transactional() {
			instance.started = true
			jobDao.update(instance)
		}
	}

	private def finish(instance: JobInstance) {
		transactional() {
			instance.finished = true
			jobDao.update(instance)
		}
	}

	/** Hmm, no Job exists to handle this JobInstance. */
	private def fail(instance: JobInstance) {
		transactional() {
			instance.succeeded = false
			instance.finished = true
			jobDao.update(instance)
		}
	}

}