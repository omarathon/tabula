package uk.ac.warwick.tabula.coursework.commands.markingworkflows

import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.{FirstMarkerOnlyWorkflow, Exam}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.{MarkingWorkflowDao, MarkingWorkflowDaoComponent}

class DeleteMarkingWorkflowCommandTest extends TestBase with Mockito {

	val mockMarkingWorkflowDao = smartMock[MarkingWorkflowDao]

	trait Fixture {

		val thisMarkingWorkflow = new FirstMarkerOnlyWorkflow
		val dept = Fixtures.department("its")

		val validator = new DeleteMarkingWorkflowCommandValidation with DeleteMarkingWorkflowCommandState with MarkingWorkflowDaoComponent
		{
			val markingWorkflowDao = mockMarkingWorkflowDao
			val department = dept
			val markingWorkflow = thisMarkingWorkflow
		}

		val command = new DeleteMarkingWorkflowCommandInternal(dept, thisMarkingWorkflow)
		val assignment1 = Fixtures.assignment("assignment1")
		var errors = new BindException(validator, "command")
	}

	@Test
	def validateInUseByAssignment(): Unit = {
		new Fixture {
			mockMarkingWorkflowDao.getAssignmentsUsingMarkingWorkflow(thisMarkingWorkflow) returns Seq(assignment1)
			mockMarkingWorkflowDao.getExamsUsingMarkingWorkflow(thisMarkingWorkflow) returns Seq()

			validator.validate(errors)
			errors.hasErrors should be {true}
			errors.getErrorCount should be (1)
		}
	}

	@Test
	def validateInUseByExams(): Unit = {
		new Fixture {
			mockMarkingWorkflowDao.getAssignmentsUsingMarkingWorkflow(thisMarkingWorkflow) returns Seq()
			mockMarkingWorkflowDao.getExamsUsingMarkingWorkflow(thisMarkingWorkflow) returns Seq(new Exam, new Exam)

			validator.validate(errors)
			errors.hasErrors should be {true}
			errors.getErrorCount should be (1)
		}
	}

	@Test
	def validateInUseByAssignmentsAndExams(): Unit = {
		new Fixture {
			mockMarkingWorkflowDao.getAssignmentsUsingMarkingWorkflow(thisMarkingWorkflow) returns Seq(assignment1)
			mockMarkingWorkflowDao.getExamsUsingMarkingWorkflow(thisMarkingWorkflow) returns Seq(new Exam)

			validator.validate(errors)
			errors.hasErrors should be {true}
			errors.getErrorCount should be (2)
		}
	}

	@Test
	def validateNotInUse(): Unit = {
		new Fixture {
			mockMarkingWorkflowDao.getAssignmentsUsingMarkingWorkflow(thisMarkingWorkflow) returns Seq()
			mockMarkingWorkflowDao.getExamsUsingMarkingWorkflow(thisMarkingWorkflow) returns Seq()

			validator.validate(errors)
			errors.hasFieldErrors should be {false}
			errors.hasErrors should be {false}
		}
	}

}
