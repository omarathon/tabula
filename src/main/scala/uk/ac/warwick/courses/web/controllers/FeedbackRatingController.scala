package uk.ac.warwick.courses.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.courses.web.Mav
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.courses.commands.feedback.RateFeedbackCommand
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.courses.actions.View

@Controller
class FeedbackRatingController extends BaseController {

	@ModelAttribute("command") def command = new RateFeedbackCommand
	
	def form: Mav = {
		mustBeAbleTo(View(command.feedback))
		Mav("submit/rating").noLayoutIf(ajax)
	}
	
	def submit(@RequestParam rating:Int): Mav = {
		form
	}
	
}