package uk.ac.warwick.tabula.commands.exams.exams

import org.springframework.validation.BindException
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.commands.exams._
import uk.ac.warwick.tabula.commands.{HasAcademicYear, SpecifiesGroupType}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services._

class EditExamCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends EditExamCommandState
		with AssessmentServiceComponent
		with UserLookupComponent
		with HasAcademicYear
		with SpecifiesGroupType
		with AssessmentMembershipServiceComponent {
			val assessmentService = mock[AssessmentService]
			val userLookup = new MockUserLookup
			val assessmentMembershipService = mock[AssessmentMembershipService]
		}

	trait Fixture {
		val module = Fixtures.module("ab123", "Test module")
		val academicYear = new AcademicYear(2014)
		val exam = Fixtures.exam("Exam 1")
		exam.module=module
		exam.academicYear=academicYear

		val command = new EditExamCommandInternal(exam) with CommandTestSupport

		val validator = new ExamValidation with EditExamCommandState with AssessmentServiceComponent with UserLookupComponent with HasAcademicYear with SpecifiesGroupType
			with AssessmentMembershipServiceComponent {
				override def exam = command.exam
				override val assessmentService = mock[AssessmentService]
				override val assessmentMembershipService = mock[AssessmentMembershipService]
				override val userLookup = mock[UserLookupService]
				override def existingGroups = None
				override def existingMembers = None
				override def updateAssessmentGroups() = List()
		}
	}

	@Test def apply() { new Fixture {
		command.name = "Exam 2"

		val examSaved = command.applyInternal()
		examSaved.name should be ("Exam 2")
		examSaved.module.code should be("ab123")
		examSaved.module.name should be("Test module")
		examSaved.academicYear.getStoreValue should be(2014)

		verify(command.assessmentService, times(1)).save(exam)
	}}

	@Test def rejectIfDuplicateName() { new Fixture {

		def name = "exam1"
		validator.name = name

		validator.assessmentService.getExamByNameYearModule(name, academicYear ,module) returns Seq(Fixtures.exam(name))

		val errors = new BindException(validator, "command")
		validator.validate(errors)

		errors.getErrorCount should be (1)
		verify(validator.assessmentService, times(1)).getExamByNameYearModule(name, academicYear ,module)
		verify(validator.assessmentService, atMost(1)).getExamByNameYearModule(any[String], any[AcademicYear] ,any[Module])
	}}
}
