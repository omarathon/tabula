package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.util.core.jodatime.DateTimeUtils
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.JavaImports._

class PublishFeedbackCommandTest extends TestBase {
	
	@Test
	def validation {
		val now = dateTime(2012, 12).withDayOfMonth(25)
		val closeDateBefore = now.minusDays(2)
		val closeDateAfter = now.plusDays(2)
		
		withFakeTime(now) {
			
			/* a whole */ new World {
				assignment.closeDate = closeDateAfter
				assignment.openEnded = false
				command.validate(errors)
				errors.hasGlobalErrors() should be (true)
				errors.getGlobalError().getCode should be ("feedback.publish.notclosed")
			}

			/* a whole */ new World {
				assignment.closeDate = closeDateBefore
				assignment.openEnded = false
				command.validate(errors)
				errors.hasGlobalErrors() should be (false)
			}
			
			/* Brave */ new World {
				assignment.openEnded = true
				command.validate(errors)
				errors.hasGlobalErrors() should be (false)
			}
			
		}
	}
	
	
	// reusable environment
	trait World {
		val assignment = newDeepAssignment(moduleCode = "IN101")
		val command = new PublishFeedbackCommand(assignment.module, assignment)
		val errors = new BindException(command, "command")
		val feedback = new Feedback()
		feedback.actualMark = Some(41)
		assignment.feedbacks = JArrayList( feedback )
	}
	
}