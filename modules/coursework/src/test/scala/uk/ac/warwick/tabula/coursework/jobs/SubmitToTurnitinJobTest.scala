package uk.ac.warwick.tabula.coursework.jobs

import org.junit.Test
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.coursework.services.turnitin._
import uk.ac.warwick.tabula.services.jobs.JobInstanceImpl
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.jobs._

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