package uk.ac.warwick.tabula.jobs

import uk.ac.warwick.tabula.services.jobs._

trait JobTestHelp {
	def createJobs: Array[Job]
	
	val dao = new MockJobDao
	lazy val service = {
		val s = new JobService
		s.jobDao = dao
		s.jobs = allJobs
		s.jobs foreach { _.promisedJobService.set(s) }
		s
	}
	
	lazy final val allJobs: Array[Job] = createJobs
	
}