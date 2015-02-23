package uk.ac.warwick.tabula.coursework.commands.exams

import uk.ac.warwick.tabula.exams.commands.{AddExamCommandState, AddExamValidation, EditExamCommandInternal, EditExamCommandState}
import uk.ac.warwick.tabula.services.{AssessmentService, AssessmentServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class EditExamCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends EditExamCommandState with AssessmentServiceComponent {
		val assessmentService = mock[AssessmentService]
	}

	trait Fixture {
		val module = Fixtures.module("ab123", "Test module")
		val academicYear = new AcademicYear(2014)
		val exam = Fixtures.exam("Exam 1")
		val command = new EditExamCommandInternal(module, academicYear, exam) with CommandTestSupport

		val validator = new AddExamValidation with AddExamCommandState {
			def module = command.module
			def academicYear = command.academicYear
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
