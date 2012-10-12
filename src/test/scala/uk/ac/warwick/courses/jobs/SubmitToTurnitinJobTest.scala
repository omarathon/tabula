package uk.ac.warwick.courses.jobs

import org.junit.Test
import uk.ac.warwick.courses._
import uk.ac.warwick.courses.services._
import uk.ac.warwick.courses.services.turnitin._
import uk.ac.warwick.courses.services.jobs.JobInstanceImpl
import uk.ac.warwick.courses.data.model.Assignment

class SubmitToTurnitinJobTest extends TestBase with Mockito with JobTestHelp {
	
	val job = new SubmitToTurnitinJob
	override def createJobs = Array[Job](job)
	
	@Test def run {
		val job = new SubmitToTurnitinJob
		job.assignmentService = mock[AssignmentService]
		job.api = mock[Turnitin]
		
		val assignment = newDeepAssignment()
		assignment.id = "12345"
		
		val s = service
			
		service.add(None, SubmitToTurnitinJob(assignment))
	}
}