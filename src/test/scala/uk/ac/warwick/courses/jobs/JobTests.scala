package uk.ac.warwick.courses.jobs

import org.junit.Test
import uk.ac.warwick.courses.TestBase
import uk.ac.warwick.courses.services.jobs._
import org.junit.Before

class JobTests extends TestBase {
	
	val testingJob = new TestingJob
	val allJobs = Array[Job](testingJob)
	
	val dao = new MockJobDao
	val service = new JobService
	service.jobs = allJobs
	service.jobDao = dao
	
	@Before def setup {
		dao.clear
	}
	
	@Test def testingJobTest {		
		dao.findOutstandingInstances(10).size should be (0)
		val id = service.add(TestingJob("Magic"))
		dao.findOutstandingInstances(10).size should be (1)
		val myInstance = service.getInstance(id).get
		myInstance.started should be (false)
		service.run
		dao.findOutstandingInstances(10).size should be (0)
	} 
	
}