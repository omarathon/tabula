package uk.ac.warwick.courses.commands.feedback

import uk.ac.warwick.courses.TestBase
import org.junit.Test
import org.springframework.validation.BindException
import uk.ac.warwick.courses.Mockito
import uk.ac.warwick.courses.data.model._

class RateFeedbackCommandTest extends TestBase with Mockito {
	@Test def nullRating {
		val (feedback,_,_,_) = deepFeedback
		val command = new RateFeedbackCommand(feedback, emptyFeatures)
		command.rating = null
		command.unset = true
		val errors = new BindException(command, "command") 
		command.validate(errors)
		errors.hasErrors should be (false)
	}
	
	@Test def invalidRating {
		val (feedback,_,_,_) = deepFeedback
		val command = new RateFeedbackCommand(feedback, emptyFeatures)
		command.rating = 0
		val errors = new BindException(command, "command") 
		command.validate(errors)
		errors.hasFieldErrors("rating") should be (true)
	}
	
	def deepFeedback = {
		val feedback = smartMock[Feedback]
		val assignment = smartMock[Assignment]
		val module = smartMock[Module]
		val department = smartMock[Department]
		feedback.assignment returns assignment
		assignment.module returns module
		module.department returns department
		department.collectFeedbackRatings returns true
		(feedback, assignment, module, department)
	}
}