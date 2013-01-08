package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.util.core.jodatime.DateTimeUtils
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.helpers.ArrayList

class PublishFeedbackCommandTest extends TestBase {
	
	@Test
	def validation {
		val now = dateTime(2012, 12).withDayOfMonth(25)
		val closeDateBefore = now.minusDays(2)
		val closeDateAfter = now.plusDays(2)
		
		withFakeTime(now) {
			
			/* a whole */ new World {
				assignment.closeDate = closeDateAfter
				command.validate(errors)
				errors.hasFieldErrors("assignment") should be (true)
				errors.getFieldError("assignment").getCode should be ("feedback.publish.notclosed")
			}

			/* a whole */ new World {
				assignment.closeDate = closeDateBefore
				command.validate(errors)
				errors.hasFieldErrors("assignment") should be (false)
			}
			
		}
	}
	
	
	// reusable environment
	trait World {
		val assignment = newDeepAssignment(moduleCode = "IN101")
		val command = new PublishFeedbackCommand
		val errors = new BindException(command, "command")
		command.assignment = assignment
		val feedback = new Feedback()
		feedback.actualMark = Some(41)
		assignment.feedbacks = ArrayList( feedback )
	}
	
}