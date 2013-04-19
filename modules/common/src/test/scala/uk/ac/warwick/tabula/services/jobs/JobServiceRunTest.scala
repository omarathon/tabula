package uk.ac.warwick.tabula.services.jobs

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.jobs.TestingJob
import uk.ac.warwick.tabula.data.Transactions
import uk.ac.warwick.tabula.Mockito

class JobServiceRunTest extends TestBase with Mockito {
	
	@Test def run = withUser("cuscav") {
		val service = new JobService
		
		val jobDao = mock[JobDao]
		
		val job = new TestingJob
		job.promisedJobService.set(service)
		
		service.jobDao = jobDao
		service.jobs = Array(job)
		
		val inst = service.add(Some(currentUser), TestingJob("job", 50))
		there was one(jobDao).saveJob(inst)
		
		jobDao.findOutstandingInstances(10) returns (Seq(inst))
		
		Transactions.disable {
			service.run
		}
		
		there was atLeastOne(jobDao).update(inst)
		
		inst.finished should be (true)
		inst.progress should be (100)
		inst.started should be (true)
		inst.status should be ("Finished the job!")
		inst.succeeded should be (true)
		inst.updatedDate should not be (null)
		
		// If we try and kill the instance now, then nothing will happen
		
	}

}