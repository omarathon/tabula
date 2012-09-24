package uk.ac.warwick.courses.services.jobs

import uk.ac.warwick.courses.data.Daoisms
import org.hibernate.criterion._
import org.hibernate.criterion.Restrictions._
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.jobs.Job
import uk.ac.warwick.courses.jobs.JobPrototype
import uk.ac.warwick.courses.data.Transactions
import uk.ac.warwick.courses.jobs.ObsoleteJobException
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.CurrentUser

@Service
class JobService extends HasJobDao with Transactions with Logging {

	/** Spring should wire in all beans that extend Job */
	@Autowired var jobs: Array[Job] = Array()

	def run {
		jobDao.findOutstandingInstances(10) foreach processInstance
	}

	def getInstance(id: String) = jobDao.getById(id)

	def processInstance(instance: JobInstance) {
		findJob(instance.jobType)
			.map { processInstance(instance, _) }
			.getOrElse { fail(instance) }
	}

	def processInstance(instance: JobInstance, job: Job) {
		start(instance)
		try run(instance, job)
		catch {
			case old: ObsoleteJobException => {
				logger.info("Job " + instance.id + " obsolete")
				fail(instance)
			}
			case e => {
				logger.info("Job " + instance.id + " failed", e)
				fail(instance)
			}
		}
	}

	def kill(instance: JobInstance) {
		/**
		 * TODO no handle on thread to actually kill it if it's running
		 * right now.
		 */
		fail(instance)
	}

	def unfinishedInstances = jobDao.unfinishedInstances

	def update(instance: JobInstance) = jobDao.update(instance)

	def findJob(identifier: String) =
		jobs.find(identifier == _.identifier)

	def add(user: Option[CurrentUser], prototype: JobPrototype): String = {
		if (findJob(prototype.identifier).isEmpty) {
			throw new IllegalArgumentException("No Job found to handle '%s'" format (prototype.identifier))
		}
		val instance = JobInstanceImpl.fromPrototype(prototype)
		user map { u =>
			instance.realUser = u.realId
			instance.apparentUser = u.apparentId
		}
		jobDao.saveJob(instance)
	}

	@Transactional
	def run(instance: JobInstance, job: Job) {
		job.run(instance)
		finish(instance)
	}

	@Transactional
	private def start(instance: JobInstance) {
		instance.started = true
		jobDao.update(instance)
	}

	@Transactional
	private def finish(instance: JobInstance) {
		instance.finished = true
		jobDao.update(instance)
	}

	/** Hmm, no Job exists to handle this JobInstance. */
	@Transactional
	private def fail(instance: JobInstance) {
		instance.succeeded = false
		instance.finished = true
		jobDao.update(instance)
	}

}