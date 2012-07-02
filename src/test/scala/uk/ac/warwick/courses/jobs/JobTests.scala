package uk.ac.warwick.courses.jobs

import org.junit.Test
import uk.ac.warwick.courses.TestBase
import uk.ac.warwick.courses.services.jobs._
import org.junit.Before
import uk.ac.warwick.courses.Mockito
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.userlookup.User

class JobTests extends TestBase with Mockito with JobTestHelp {
	
	val testingJob = new TestingJob
	override def createJobs = Array[Job](testingJob)
	
	@Before def setup {
		dao.clear	
		val realUser = new User("real")
		val fakeUser = new User("apparent")
		currentUser = new CurrentUser(realUser, fakeUser)
	}
	
	@Test def testingJobTest {
		dao.findOutstandingInstances(10).size should be (0)
		val id = service.add(Some(currentUser), TestingJob("Magic"))
		dao.findOutstandingInstances(10).size should be (1)
		val myInstance = service.getInstance(id).get
		myInstance.started should be (false)
		service.run
		myInstance.finished should be (true)
		myInstance.succeeded should be (true)
		dao.findOutstandingInstances(10).size should be (0)
	} 
	
}