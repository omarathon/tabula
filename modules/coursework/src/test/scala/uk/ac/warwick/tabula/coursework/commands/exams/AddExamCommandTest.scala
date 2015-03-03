package uk.ac.warwick.tabula.coursework.commands.exams

import org.springframework.validation.BindException
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.{HasAcademicYear, SpecifiesGroupType}
import uk.ac.warwick.tabula.exams.commands.{AddExamCommandInternal, AddExamCommandState, ExamValidation}
import uk.ac.warwick.tabula.services._

class AddExamCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends AddExamCommandState with AssessmentServiceComponent
		with UserLookupComponent
		with HasAcademicYear
		with SpecifiesGroupType
		with AssessmentMembershipServiceComponent {
		val assessmentService = mock[AssessmentService]
		val userLookup = new MockUserLookup
		var assessmentMembershipService = mock[AssessmentMembershipService]
	}

	trait Fixture {
		val module = Fixtures.module("ab123", "Test module")
		val academicYear = new AcademicYear(2014)
		val command = new AddExamCommandInternal(module, academicYear) with CommandTestSupport

		val validator = new ExamValidation with AddExamCommandState {
			def module = command.module
			def academicYear = command.academicYear
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
