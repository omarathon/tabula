package uk.ac.warwick.tabula.jobs

import collection.mutable
import uk.ac.warwick.tabula._
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.jobs.JobService
import org.hibernate.Session
import uk.ac.warwick.tabula.services.jobs.JobInstanceImpl

class JobContextTests extends AppContextTestBase {

	@Autowired var jobService: JobService = _
	
	@Test def containsTurnitin {
		jobService.jobs.size should (be > 1)
		jobService.jobs map (_.identifier) should contain ("turnitin-submit")
	}
	
	@Test def unknownJobType {
		jobService.jobs map (_.identifier) should not contain ("unknown-job-type")
		
		val instance = JobInstanceImpl.fromPrototype(JobPrototype("unknown", Map()))
		
		val id = jobService.processInstance(instance)
		
		// Check that the flags have not actually been updated.
		withClue("Started") { instance.started should be (false) }
		withClue("Finished") { instance.finished should be (false) }
		withClue("Succeeded") { instance.succeeded should be (false) }
	}
}