package uk.ac.warwick.tabula.services.jobs

import uk.ac.warwick.tabula.data.Daoisms
import org.hibernate.criterion._
import org.hibernate.criterion.Restrictions._
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.jobs.Job
import uk.ac.warwick.tabula.jobs.JobPrototype
import uk.ac.warwick.tabula.data.Transactions
import uk.ac.warwick.tabula.jobs.ObsoleteJobException
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.jobs.FailedJobException

@Service
class JobService extends HasJobDao with Logging {
	import Transactions._
	
	// How many jobs to load and run each time
	val RunBatchSize = 10

	/** Spring should wire in all beans that extend Job */
	@Autowired var jobs: Array[Job] = Array()

	def run {
		jobDao.findOutstandingInstances(RunBatchSize).par foreach processInstance
	}

	def getInstance(id: String) = jobDao.getById(id)

	def processInstance(instance: JobInstance) {
		findJob(instance.jobType).getOrElse({logger.warn("Couldn't find a job matching for this instance: " + instance)}) match {
			case job: Job => {
				processInstance(instance, job)
			}
			case _ => 
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
		if (findJob(prototype.identifier).isEmpty) {
			throw new IllegalArgumentException("No Job found to handle '%s'" format (prototype.identifier))
		}
		
		val instance = JobInstanceImpl.fromPrototype(prototype)
		user map { u =>
			instance.realUser = u.realId
			instance.apparentUser = u.apparentId
		}
		
		// Do we already have an outstanding job with these details? Outstanding; just return that.
		jobDao.findOutstandingInstance(instance) getOrElse {
			jobDao.saveJob(instance)
			
			instance
		}
	}

	def run(instance: JobInstance, job: Job) {
		transactional() {
			try job.run(instance)
			catch {
				case old: ObsoleteJobException => {
					logger.info("Job " + instance.id + " obsolete")
					fail(instance)
				}
				case failed: FailedJobException => {
					logger.info("Job " + instance.id + " failed: " + failed.status)
					instance.status = failed.status
					fail(instance)
				}
				case e: Throwable => {
					logger.info("Job " + instance.id + " failed", e)
					instance.status = "Sorry, there was an error: " + e.getMessage()
					fail(instance)
				}
			}
			finish(instance)
		}
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