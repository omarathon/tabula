package uk.ac.warwick.courses.jobs

import uk.ac.warwick.courses.jobs._
import uk.ac.warwick.courses.services.jobs._
import org.springframework.stereotype.Component
import org.springframework.stereotype.Component

object TestingJob {
	val id = "testing"
	def apply(name: String, sleepTime: Int = 0) = JobPrototype(id, Map(
		"name" -> name,
		"sleepTime" -> sleepTime))
}

@Component
class TestingJob extends Job {
	val identifier = TestingJob.id

	def run(implicit job: JobInstance) {
		val name = job.getString("name")
		val sleepTime = job.getString("sleepTime").toInt
		updateStatus("Running the job with name %s." format (name))
		for (i <- 1 to 50) {
			updateProgress(i*2)
			if (sleepTime != 0) Thread.sleep(50)
		}
		job.succeeded = true
		updateStatus("Finished the job!")
	}

}

