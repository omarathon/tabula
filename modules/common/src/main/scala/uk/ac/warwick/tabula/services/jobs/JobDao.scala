package uk.ac.warwick.tabula.services.jobs

import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.Daoisms
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.Transactions._

/**
 * Provides low level access to JobDefinitions in the database.
 */
trait JobDao {
	def findOutstandingInstances(max: Int): Seq[JobInstance]
	def saveJob(instance: JobInstance): String
	def getById(id: String): Option[JobInstance]
	def unfinishedInstances: Seq[JobInstance]
	def listRecent(start: Int, count: Int): Seq[JobInstance]
	def update(instance: JobInstance): Unit
}

trait HasJobDao {
	@Autowired var jobDao: JobDao = _
}

@Service
class JobDaoImpl extends JobDao with Daoisms {
	import org.hibernate.criterion.Order._

	def findOutstandingInstances(max: Int): Seq[JobInstance] =
		transactional(readOnly = true) {
			session.newCriteria[JobInstanceImpl]
				.add(is("started", false))
				.setMaxResults(max)
				.seq
		}

	def getById(id: String) = transactional(readOnly = true) {
		getById[JobInstanceImpl](id)
	}

	def saveJob(instance: JobInstance) = transactional() {
		instance match {
			case instance: JobInstanceImpl => {
				session.save(instance)
				instance.id
			}
			case _ => throw new IllegalArgumentException("JobDaoImpl only accepts JobInstanceImpls")
		}
	}

	def update(instance: JobInstance) = transactional() {
		instance match {
			case instance: JobInstanceImpl => session.update(instance)
			case _ => throw new IllegalArgumentException("JobDaoImpl only accepts JobInstanceImpls")
		}
	}

	def unfinishedInstances: Seq[JobInstance] =
		session.newCriteria[JobInstanceImpl]
			.add(is("finished", false))
			.addOrder(desc("createdDate"))
			.seq
			
	def listRecent(start: Int, count: Int): Seq[JobInstance] =
		session.newCriteria[JobInstanceImpl]
			.add(is("finished", true))
			.addOrder(desc("createdDate"))
			.setFirstResult(start)
			.setMaxResults(count)
			.seq

}