package uk.ac.warwick.tabula.coursework.commands.exams

import uk.ac.warwick.tabula.commands.{HasAcademicYear, SpecifiesGroupType}
import uk.ac.warwick.tabula.exams.commands._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class EditExamCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends EditExamCommandState
		with AssessmentServiceComponent
		with UserLookupComponent
		with HasAcademicYear
		with SpecifiesGroupType
		with AssessmentMembershipServiceComponent {
			val assessmentService = mock[AssessmentService]
			val assessmentMembershipService = mock[AssessmentMembershipService]
			val userLookup = mock[UserLookupService]
	}

	trait Fixture {
		val module = Fixtures.module("ab123", "Test module")
		val academicYear = new AcademicYear(2014)
		val exam = Fixtures.exam("Exam 1")
		exam.module=module
		exam.academicYear=academicYear

		val command = new EditExamCommandInternal(exam) with CommandTestSupport

		val validator = new ExamValidation with EditExamCommandState {
				override def exam = command.exam
		}
	}

	@Test def apply { new Fixture {
		command.name = "Exam 2"

		val examSaved = command.applyInternal()
		examSaved.name should be ("Exam 2")
		examSaved.module.code should be("ab123")
		examSaved.module.name should be("Test module")
		examSaved.academicYear.getStoreValue should be(2014)

		there was one (command.assessmentService).save(exam)
	}}
}
