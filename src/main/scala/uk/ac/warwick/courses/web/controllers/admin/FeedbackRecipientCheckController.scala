package uk.ac.warwick.courses.web.controllers.admin

import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import uk.ac.warwick.courses.commands.FeedbackRecipientCheckCommand
import uk.ac.warwick.courses.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.courses.actions.Participate

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/check-recipients"))
class FeedbackRecipientCheckController extends BaseController {
	
	@RequestMapping()
	def confirmation(command:FeedbackRecipientCheckCommand, errors:Errors): Mav = {
	   mustBeLinked(command.assignment, command.module)
	   mustBeAbleTo(Participate(command.module))
	   val report = command.apply()
	   Mav("admin/assignments/publish/checkrecipients", 
	       "assignment" -> command.assignment,
	       "report" -> report).noLayout
	}
	
}