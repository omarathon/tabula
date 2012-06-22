package uk.ac.warwick.courses.jobs

import collection.mutable
import uk.ac.warwick.courses._
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.jobs.JobService
import org.hibernate.Session
import uk.ac.warwick.courses.services.jobs.JobInstanceImpl

class JobContextTests extends AppContextTestBase {

	@Autowired var jobService: JobService = _

	@Test def jobInstanceSerialization {
		val id = transactional { t => 
			val jsi = new JobInstanceImpl
			jsi.data = """{"How" : "Data"}"""
			jsi.json = Map("How" -> "Json")
			jsi.succeeded = true
			session.save(jsi)
			jsi.id
		}
		transactional { t =>
			val jsiLoaded = session.get(classOf[JobInstanceImpl], id).asInstanceOf[JobInstanceImpl]
			jsiLoaded.data should be ("""{"How":"Json"}""")
			jsiLoaded.succeeded should be (true)
		}
	}
	
	@Test def load {
		jobService.jobs.size should (be > 1)
		jobService.jobs map (_.identifier) should contain ("turnitin-submit")
		
		val id = jobService.add(TestingJob("anything really"))
		jobService.getInstance(id) map { instance =>
			jobService.run
		} orElse fail()
		
//		transactional { _ => session.clear }
		
		// Check that the flags have actually been updated.
		jobService.getInstance(id) map { instance =>
			withClue("Started") { instance.started should be (true) }
			withClue("Finished") { instance.finished should be (true) }
			withClue("Succeeded") { instance.succeeded should be (true) }
		}
	}
}