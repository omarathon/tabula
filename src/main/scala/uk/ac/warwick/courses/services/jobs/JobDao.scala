package uk.ac.warwick.courses.services.jobs

import org.springframework.stereotype.Service
import uk.ac.warwick.courses.data.Daoisms
import org.springframework.transaction.annotation.Transactional
import org.springframework.beans.factory.annotation.Autowired

/**
 * Provides low level access to JobDefinitions in the database.
 */
trait JobDao {
	def findOutstandingInstances(max: Int): Seq[JobInstance]
	def saveJob(instance: JobInstance): String
	def getById(id: String): Option[JobInstance]
	def unfinishedInstances: Seq[JobInstance]
	def update(instance: JobInstance): Unit
}

trait HasJobDao {
	@Autowired var jobDao: JobDao = _
}

@Service
class JobDaoImpl extends JobDao with Daoisms {

	@Transactional(readOnly = true)
	def findOutstandingInstances(max: Int): Seq[JobInstance] =
		session.newCriteria[JobInstanceImpl]
			.add(is("started", false))
			.setMaxResults(max)
			.seq

	@Transactional(readOnly = true)
	def getById(id: String) = getById[JobInstanceImpl](id)

	@Transactional
	def saveJob(instance: JobInstance) = instance match {
		case instance: JobInstanceImpl => {
			session.save(instance)
			instance.id
		}
		case _ => throw new IllegalArgumentException("JobDaoImpl only accepts JobInstanceImpls")
	}

	@Transactional
	def update(instance: JobInstance) = instance match {
		case instance: JobInstanceImpl => session.update(instance)
		case _ => throw new IllegalArgumentException("JobDaoImpl only accepts JobInstanceImpls")
	}

	def unfinishedInstances: Seq[JobInstance] =
		session.newCriteria[JobInstanceImpl]
			.add(is("finished", false))
			.seq

}