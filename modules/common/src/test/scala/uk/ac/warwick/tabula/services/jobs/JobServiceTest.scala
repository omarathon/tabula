package uk.ac.warwick.tabula.services.jobs
import uk.ac.warwick.tabula.jobs.TestingJob
import uk.ac.warwick.tabula.{Mockito, TestBase}

// scalastyle:off magic.number
class JobServiceTest extends TestBase with Mockito {
	
	val service = new JobService
	val jobDao = smartMock[JobDao]
	service.jobDao = jobDao
	
	@Test def add() {
		service.jobs = Array()
		jobDao.findOutstandingInstance(any[JobInstanceImpl]) throws new IllegalArgumentException(s"No Job found to handle '${TestingJob("job").identifier}'")

		service.jobs = Array(new TestingJob)
		jobDao.findOutstandingInstance(any[JobInstanceImpl]) returns None
		val inst = service.add(None, TestingJob("job"))
		there was one(jobDao).saveJob(inst)
	}
	
	@Test def run() = withUser("cuscav") {
		jobDao.findOutstandingInstance(any[JobInstance]) returns None
		
		val job = new TestingJob
		job.jobService = service
		
		service.jobDao = jobDao
		service.jobs = Array(job)
		
		val inst = service.add(Some(currentUser), TestingJob("job", 50))
		there was one(jobDao).saveJob(inst)
		
		jobDao.findOutstandingInstances(10) returns Seq(inst)

		service.run()
		
		there was atLeastOne(jobDao).update(inst)
		
		inst.finished should be {true}
		inst.progress should be (100)
		inst.started should be {true}
		inst.status should be ("Finished the job!")
		inst.succeeded should be {true}
		inst.updatedDate should not be null
		
		// If we try and kill the instance now, then nothing will happen
		
	}
	
	@Test def kill() = withUser("cuscav") {
		jobDao.findOutstandingInstance(any[JobInstance]) returns None
		
		val job = new TestingJob
		job.jobService = service
		
		service.jobDao = jobDao
		service.jobs = Array(job)
		
		val inst = service.add(Some(currentUser), TestingJob("job"))
		there was one(jobDao).saveJob(inst)
		
		jobDao.findOutstandingInstances(10) returns Seq(inst)
		
		service.kill(inst)
		
		there was atLeastOne(jobDao).update(inst)
		
		inst.progress should be (0)
		inst.status should be ("Killed")
		inst.finished should be {true}
		inst.succeeded should be {false}
		inst.updatedDate should not be null
	}

}