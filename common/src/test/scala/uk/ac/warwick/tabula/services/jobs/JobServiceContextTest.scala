package uk.ac.warwick.tabula.services.jobs

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.jobs.JobPrototype
import uk.ac.warwick.tabula.{AppContextTestBase, EarlyRequestInfoImpl}

class JobServiceContextTest extends AppContextTestBase {

  @Autowired var jobService: JobService = _

  @Test def containsSubmissionZipJob(): Unit = {
    jobService.jobs.length should (be > 1)
    jobService.jobs map (_.identifier) should contain("submission-zip-file")
  }

  @Test def unknownJobType(): Unit = {
    jobService.jobs map (_.identifier) should not contain "unknown-job-type"

    val instance = JobInstanceImpl.fromPrototype(JobPrototype("unknown", Map()))

    jobService.processInstance(instance)(new EarlyRequestInfoImpl)

    // Check that the flags have not actually been updated.
    withClue("Started") {
      instance.started should be (false)
    }
    withClue("Finished") {
      instance.finished should be (false)
    }
    withClue("Succeeded") {
      instance.succeeded should be (false)
    }
  }
}
