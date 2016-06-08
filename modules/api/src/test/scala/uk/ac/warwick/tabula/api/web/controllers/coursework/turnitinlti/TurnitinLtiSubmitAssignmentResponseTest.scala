package uk.ac.warwick.tabula.api.web.controllers.coursework.turnitinlti

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.services.{AssessmentService, AssessmentServiceComponent}
import uk.ac.warwick.tabula.services.turnitinlti.{TurnitinLtiQueueService, TurnitinLtiQueueServiceComponent, TurnitinLtiService}
import uk.ac.warwick.tabula.api.commands.coursework.turnitinlti.{TurnitinLtiSubmitAssignmentResponseCommandInternal, TurnitinLtiSubmitAssignmentResponseCommandState, TurnitinLtiSubmitAssignmentResponseRequestState, TurnitinLtiSubmitAssignmentResponseValidation}

class TurnitinLtiSubmitAssignmentResponseTest extends TestBase with Mockito {

	trait CommandTestSupport extends TurnitinLtiSubmitAssignmentResponseRequestState
		with TurnitinLtiSubmitAssignmentResponseValidation
		with TurnitinLtiSubmitAssignmentResponseCommandState
		with TurnitinLtiQueueServiceComponent
		with AssessmentServiceComponent {
			val assessmentService = smartMock[AssessmentService]
			val turnitinLtiQueueService = smartMock[TurnitinLtiQueueService]
		}

	trait Fixture {
		val assignment = Fixtures.assignment("an assignment")
		val anotherAssignment = Fixtures.assignment("another assignment")

		val command = new TurnitinLtiSubmitAssignmentResponseCommandInternal(assignment) with CommandTestSupport
		command.assessmentService.getAssignmentById("1234") returns Some(assignment)
		command.assessmentService.getAssignmentById("4321") returns Some(anotherAssignment)
		command.assessmentService.getAssignmentById("9999") returns None
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
		intercept[NoSuchElementException] {
			command.validate(errors)
		}
	}}

}
