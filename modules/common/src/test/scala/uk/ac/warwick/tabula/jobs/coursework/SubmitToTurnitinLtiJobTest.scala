package uk.ac.warwick.tabula.jobs.coursework

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.jobs._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.turnitinlti.TurnitinLtiService

class SubmitToTurnitinLtiJobTest extends TestBase with Mockito with JobTestHelp {

	val job = new SubmitToTurnitinLtiJob
	override def createJobs = Array[Job](job)

	@Test def run(): Unit = {
		val job = new SubmitToTurnitinLtiJob
		job.assessmentService = mock[AssessmentService]
		job.turnitinLtiService = smartMock[TurnitinLtiService]

		val assignment = newDeepAssignment()
		assignment.id = "12345"

		service.add(None, SubmitToTurnitinLtiJob(assignment))
	}
}