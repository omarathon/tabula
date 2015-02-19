package uk.ac.warwick.tabula.coursework.commands.exams

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.commands.CurrentSITSAcademicYear
import uk.ac.warwick.tabula.exams.commands.{AddExamValidation, AddExamCommandInternal, AddExamCommandState}
import uk.ac.warwick.tabula.services.{AssessmentService, AssessmentServiceComponent}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class AddExamCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends AddExamCommandState with CurrentSITSAcademicYear with AssessmentServiceComponent {
		val assessmentService = mock[AssessmentService]
	}

	trait Fixture {
		val amodule = Fixtures.module("ab123", "Test module")
		val command = new AddExamCommandInternal(amodule) with CommandTestSupport

		val validator = new AddExamValidation with AddExamCommandState {
			def module = command.module
		}

	}

	@Test def apply { new Fixture {
		command.name = "Some exam"

		val exam = command.applyInternal()
		exam.name should be ("Some exam")

		there was one (command.assessmentService).save(exam)
	}}

	@Test def rejectEmptyCode { new Fixture {

		validator.name = "    "

		val errors = new BindException(validator, "command")
		validator.validate(errors)

		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("exam.name.empty")
	}}

	@Test def validateValid { new Fixture {

		validator.name = "ab123"

		val errors = new BindException(validator, "command")
		validator.validate(errors)

		errors.getErrorCount should be (0)
	}}
}
