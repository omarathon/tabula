package uk.ac.warwick.tabula.services.jobs
import org.springframework.beans.factory.annotation.Autowired



import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.jobs.TestingJob

class JobServiceTest extends AppContextTestBase {
	
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

}