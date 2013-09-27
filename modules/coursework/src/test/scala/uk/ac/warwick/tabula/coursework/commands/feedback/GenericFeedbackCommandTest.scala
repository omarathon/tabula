package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services.{ AssignmentService, AssignmentServiceComponent}
import uk.ac.warwick.tabula.data.model.{Module, Assignment}


class GenericFeedbackCommandTest extends TestBase with Mockito {

	trait Fixture {
		val assignment = new Assignment
		val module = new Module
		assignment.module = module

		val heronRant = "A common mistake that most of you made; was to class herons as Avialae. They are actualy 'Rancid winged devils'"
		val command = new GenericFeedbackCommand(module, assignment) with GenericFeedbackCommandTestSupport
		command.genericFeedback = heronRant
	}


	@Test
	def commandApply() {
		new Fixture {
			assignment.genericFeedback should be("")
			val result = command.applyInternal()
			there was one(command.assignmentService).save(assignment)
			assignment.genericFeedback should be(heronRant)
		}
	}
}

// Implements the dependencies declared by the command
trait GenericFeedbackCommandTestSupport extends AssignmentServiceComponent with Mockito {
	val assignmentService = mock[AssignmentService]
	def apply(): Assignment = null
}
