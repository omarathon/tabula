package uk.ac.warwick.courses.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.courses.web.Mav
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.courses.commands.feedback.RateFeedbackCommand
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.courses.actions.View
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Feedback
import uk.ac.warwick.courses.Features
import org.springframework.beans.factory.annotation.Autowired
import scala.reflect.BeanProperty
import uk.ac.warwick.courses.data.model.Module
import org.springframework.web.bind.annotation.RequestMethod._
import org.springframework.validation.Errors

@RequestMapping(Array("/module/{module}/{assignment}/rate"))
@Controller
class FeedbackRatingController extends AbstractAssignmentController {

	@Autowired @BeanProperty var features: Features = _

	hideDeletedItems

	@ModelAttribute def cmd(
		@PathVariable("assignment") assignment: Assignment,
		@PathVariable("module") module: Module) = {
		mustBeLinked(assignment, module)
		new RateFeedbackCommand(mandatory(checkCanGetFeedback(assignment, user).orNull), features)
	}

	@RequestMapping(method = Array(GET, HEAD))
	def form(command: RateFeedbackCommand): Mav = {
		mustBeAbleTo(View(command.feedback))
		Mav("submit/rating").noLayoutIf(ajax)
	}

	@RequestMapping(method = Array(POST))
	def submit(command: RateFeedbackCommand, errors: Errors): Mav = {
		command.validate(errors)
		if (errors.hasErrors) {
			form(command)
		} else {
			command.apply()
			Mav("submit/rating", "rated" -> true).noLayoutIf(ajax)
		}
	}

}