package uk.ac.warwick.courses.jobs

import uk.ac.warwick.courses._
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.jobs.JobService

class JobContextTests extends AppContextTestBase {

	@Autowired var jobService: JobService = _

	@Test def load {
		jobService.jobs.size should (be > 1)
		jobService.jobs map (_.identifier) should contain ("turnitin-submit")
		
		val id = jobService.add(TestingJob("anything really"))
		jobService.getInstance(id) map { instance =>
			jobService.run
		} orElse fail()
		
	}
}