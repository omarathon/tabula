package uk.ac.warwick.tabula.services.jobs
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.jobs.TestingJob
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.data.Transactions

class JobServiceTest extends AppContextTestBase with Mockito {
	
	lazy val service = Wire[JobService]
	
	@Test def crud {
		var inst = transactional { t => service.add(None, TestingJob("job")) }
		
		transactional { t =>
			service.getInstance(inst.id) should be (Some(inst))
			
			inst = service.getInstance(inst.id).get
					
			service.unfinishedInstances.length should be (1)
			service.unfinishedInstances.contains(inst) should be (true)
			service.listRecent(0, 5).length should be (0)
			service.listRecent(0, 5).contains(inst) should be (false)
			
			inst.finished = true
			service.update(inst)
		}
		
		transactional { t =>
			service.unfinishedInstances.length should be (0)
			service.unfinishedInstances.contains(inst) should be (false)
			service.listRecent(0, 5).length should be (1)
			service.listRecent(0, 5).contains(inst) should be (true)
		}
	}
	
	@Test def kill = withUser("cuscav") {
		val jobDao = mock[JobDao]
		
		val job = new TestingJob
		
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