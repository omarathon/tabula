package uk.ac.warwick.tabula.services.jobs
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.jobs.TestingJob
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.Transactions

class JobServiceTest extends AppContextTestBase with Mockito {
	
	@Autowired var service: JobService = _
	
	@Test def crud = transactional { t =>
		var inst = service.add(None, TestingJob("job"))
		
		session.flush()
		session.clear()
		
		service.getInstance(inst.id) should be (Some(inst))
		
		inst = service.getInstance(inst.id).get
				
		service.unfinishedInstances.length should be (1)
		service.unfinishedInstances.contains(inst) should be (true)
		service.listRecent(0, 5).length should be (0)
		service.listRecent(0, 5).contains(inst) should be (false)
		
		inst.finished = true
		service.update(inst)
		
		service.unfinishedInstances.length should be (0)
		service.unfinishedInstances.contains(inst) should be (false)
		service.listRecent(0, 5).length should be (1)
		service.listRecent(0, 5).contains(inst) should be (true)
	}
	
	@Test def run = withUser("cuscav") {
		val service = new JobService
		val jobDao = mock[JobDao]
		
		val job = new TestingJob
		job.jobService = service
		
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
	
	
	
	@Test def kill = withUser("cuscav") {
		val service = new JobService
		val jobDao = mock[JobDao]
		
		val job = new TestingJob
		job.jobService = service
		
		service.jobDao = jobDao
		service.jobs = Array(job)
		
		val inst = service.add(Some(currentUser), TestingJob("job"))
		there was one(jobDao).saveJob(inst)
		
		jobDao.findOutstandingInstances(10) returns (Seq(inst))
		
		Transactions.disable {
			service.kill(inst)
		}
		
		there was atLeastOne(jobDao).update(inst)
		
		inst.progress should be (0)
		inst.status should be ("Killed")
		inst.finished should be (true)
		inst.succeeded should be (false)
		inst.updatedDate should not be (null)
	}

}