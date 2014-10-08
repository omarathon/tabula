package uk.ac.warwick.tabula.jobs

import uk.ac.warwick.tabula._
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.jobs.JobService
import uk.ac.warwick.tabula.services.jobs.JobInstanceImpl

class JobContextTests extends AppContextTestBase {

	@Autowired var jobService: JobService = _

	@Test def jobInstanceSerialization() {
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
			jsiLoaded.succeeded should be {true}
		}
	}
	
	@Test def load() {
		val id = jobService.add(None, TestingJob("anything really")).id
		jobService.getInstance(id) map { instance =>
			jobService.run()
		} orElse fail()
		
		// Check that the flags have actually been updated.
		jobService.getInstance(id) map { instance =>
			
			withClue("Started") { instance.started should be {true} }
			withClue("Finished") { instance.finished should be {true} }
			withClue("Succeeded") { instance.succeeded should be {true} }
		}
	}
}