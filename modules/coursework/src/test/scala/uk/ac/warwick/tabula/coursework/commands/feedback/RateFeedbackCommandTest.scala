package uk.ac.warwick.tabula.coursework.commands.feedback

import collection.JavaConversions._
import uk.ac.warwick.tabula.coursework.TestBase
import uk.ac.warwick.tabula.JavaImports._
import org.junit.Test
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.coursework.Mockito
import uk.ac.warwick.tabula.coursework.data.model._


import org.springframework.web.bind.WebDataBinder
import org.springframework.validation.DataBinder
import org.springframework.beans.PropertyValues
import org.springframework.beans.MutablePropertyValues
import org.springframework.web.bind.ServletRequestDataBinder

class RateFeedbackCommandTest extends TestBase with Mockito {
	@Test def nullRating {
		val (feedback,_,_,_) = deepFeedback
		val command = new RateFeedbackCommand(feedback, emptyFeatures)
		command.wasPrompt.value = null
		command.wasPrompt.unset = true
		command.wasHelpful.value = null
		command.wasHelpful.unset = true
		val errors = new BindException(command, "command") 
		command.validate(errors)
		withClue(errors) { errors.hasErrors should be (false) }
	}
	
	@Test def invalidRating {
		val (feedback,_,_,_) = deepFeedback
		val command = new RateFeedbackCommand(feedback, emptyFeatures)
		command.wasPrompt.value = null
		command.wasPrompt.unset = false
		val errors = new BindException(command, "command") 
		command.validate(errors)
		errors.hasFieldErrors("wasPrompt") should be (true)
	}

	def deepFeedback = {
		val feedback = smartMock[Feedback]
		val assignment = smartMock[Assignment]
		val module = smartMock[Module]
		val department = smartMock[Department]
		feedback.collectRatings returns true
		feedback.ratingHelpful returns None
		feedback.ratingPrompt returns None
		feedback.assignment returns assignment
		assignment.module returns module
		module.department returns department
		department.collectFeedbackRatings returns true
		(feedback, assignment, module, department)
	}
}
