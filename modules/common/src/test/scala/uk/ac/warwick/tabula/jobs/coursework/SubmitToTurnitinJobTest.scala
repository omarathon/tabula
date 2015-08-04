package uk.ac.warwick.tabula.jobs.coursework

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.services.turnitin.Turnitin
import uk.ac.warwick.tabula.jobs._
import uk.ac.warwick.tabula.services._

class SubmitToTurnitinJobTest extends TestBase with Mockito with JobTestHelp {
	
	val job = new SubmitToTurnitinJob
	override def createJobs = Array[Job](job)
	
	@Test def run(): Unit = {
		val job = new SubmitToTurnitinJob
		job.assignmentService = mock[AssessmentService]
		job.api = smartMock[Turnitin]
		
		val assignment = newDeepAssignment()
		assignment.id = "12345"
		
		val s = service
			
		service.add(None, SubmitToTurnitinJob(assignment))
	}
}