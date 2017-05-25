package uk.ac.warwick.tabula.commands.cm2.feedback

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.services.{ AssessmentService, AssessmentServiceComponent}
import uk.ac.warwick.tabula.data.model.Assignment


class GenericFeedbackCommandTest extends TestBase with Mockito {

	trait Fixture {
		val assignment = new Assignment
		val heronRant = "A common mistake that most of you made; was to class herons as Avialae. They are actualy 'Rancid winged devils'"
		val command = new GenericFeedbackCommand(assignment) with GenericFeedbackCommandTestSupport
		command.genericFeedback = heronRant
	}


	@Test
	def commandApply() {
		new Fixture {
			assignment.genericFeedback should be("")
			val result: Assignment = command.applyInternal()
			verify(command.assessmentService, times(1)).save(assignment)
			assignment.genericFeedback should be(heronRant)
		}
	}
}

// Implements the dependencies declared by the command
trait GenericFeedbackCommandTestSupport extends AssessmentServiceComponent with Mockito {
	val assessmentService: AssessmentService = mock[AssessmentService]
	def apply(): Assignment = null
}
