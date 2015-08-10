package uk.ac.warwick.tabula.api.web.controllers.coursework.turnitinlti

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.services.{AssessmentService, AssessmentServiceComponent}
import uk.ac.warwick.tabula.services.turnitinlti.TurnitinLtiService
import uk.ac.warwick.tabula.api.commands.coursework.turnitinlti.{TurnitinLtiSubmitAssignmentResponseRequestState, TurnitinLtiSubmitAssignmentResponseCommandState, TurnitinLtiSubmitAssignmentResponseValidation, TurnitinLtiSubmitAssignmentResponseCommandInternal}

class TurnitinLtiSubmitAssignmentResponseTest extends TestBase with Mockito {

	trait CommandTestSupport extends TurnitinLtiSubmitAssignmentResponseRequestState
		with TurnitinLtiSubmitAssignmentResponseValidation
		with TurnitinLtiSubmitAssignmentResponseCommandState
		with AssessmentServiceComponent {
			val assessmentService = mock[AssessmentService]
		}

	trait Fixture {
		val assignment = Fixtures.assignment("an assignment")
		val anotherAssignment = Fixtures.assignment("another assignment")

		val command = new TurnitinLtiSubmitAssignmentResponseCommandInternal(assignment) with CommandTestSupport
		command.assessmentService.getAssignmentById("1234") returns Some(assignment)
		command.assessmentService.getAssignmentById("4321") returns Some(anotherAssignment)
	}

	@Test
	def validateValid() { new Fixture {
		var errors = new BindException(command, "command")
		command.resource_link_id = s"${TurnitinLtiService.AssignmentPrefix}1234"
		command.validate(errors)
		errors.hasFieldErrors should be {false}
	}}

	@Test
	def validateIncorrectAssignment() { new Fixture {
		var errors = new BindException(command, "command")
		command.resource_link_id = s"${TurnitinLtiService.AssignmentPrefix}4321"
		command.validate(errors)
		errors.hasFieldErrors should be {true}
	}}

	@Test
	def validateUnknownAssignment() { new Fixture {
		var errors = new BindException(command, "command")
		command.resource_link_id = s"${TurnitinLtiService.AssignmentPrefix}9999"
		try {
			command.validate(errors)
			fail("Should throw an exception")
		} catch { case e: NullPointerException => }

	}}

}
