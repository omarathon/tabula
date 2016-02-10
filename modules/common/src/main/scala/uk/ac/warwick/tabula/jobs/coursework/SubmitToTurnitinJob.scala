package uk.ac.warwick.tabula.jobs.coursework

import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.jobs._
import uk.ac.warwick.tabula.services.jobs.JobInstance

object SubmitToTurnitinJob {
	val identifier = "turnitin-submit"
}

@Component
@deprecated("Only here so the job service doesn't blow up trying to inflate an old job", "114")
class SubmitToTurnitinJob extends Job {
	val identifier = SubmitToTurnitinJob.identifier

	def run(implicit job: JobInstance) = {
		throw new IllegalStateException("Old-style Turnitin submission is no longer supported")
	}
}