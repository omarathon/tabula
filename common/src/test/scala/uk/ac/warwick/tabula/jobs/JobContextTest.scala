package uk.ac.warwick.tabula.jobs

import org.junit.Ignore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.services.jobs._

class JobContextTest extends AppContextTestBase {

  @Autowired var jobService: JobService = _

  @Transactional
  @Test
  def jobInstanceSerialization() {
    val id = {
      val jsi = new JobInstanceImpl
      jsi.jobType = "Steven"
      jsi.data = """{"How" : "Data"}"""
      jsi.json = Map("How" -> "Json")
      jsi.succeeded = true
      session.save(jsi)
      session.flush()
      session.clear()
      jsi.id
    }

    val jsiLoaded = session.get(classOf[JobInstanceImpl], id)
    jsiLoaded.data should be("""{"How":"Json"}""")
    jsiLoaded.succeeded should be (true)
  }

  @Transactional
  @Ignore
  @Test
  def load() {
    val id = jobService.add(None, TestingJob("anything really")).id
    jobService.getInstance(id).map { _ =>
      jobService.run()(new EarlyRequestInfoImpl)
    }.orElse(fail())

    // Check that the flags have actually been updated.
    jobService.getInstance(id).map { instance =>
      withClue("Started") {
        instance.started should be (true)
      }
      withClue("Finished") {
        instance.finished should be (true)
      }
      withClue("Succeeded") {
        instance.succeeded should be (true)
      }
    }
  }

}