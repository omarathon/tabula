package uk.ac.warwick.courses.jobs

import uk.ac.warwick.courses.jobs._
import uk.ac.warwick.courses.services.jobs._
import org.springframework.stereotype.Component
import org.springframework.stereotype.Component

object TestingJob {
	val id = "testing" 
	def apply(name:String) = JobPrototype(id, Map("name" -> name))
}

@Component
class TestingJob extends Job {
	val identifier = TestingJob.id
	
	def run(implicit job: JobInstance) {
		val name = job.getString("name")
		status = "Running the job with name %s." format (name)
		for (i <- 1 to 50) {
			progress = i*2
			//Thread.sleep(50)
		}
		job.succeeded = true
		status = "Finished the job!"
	}

}

