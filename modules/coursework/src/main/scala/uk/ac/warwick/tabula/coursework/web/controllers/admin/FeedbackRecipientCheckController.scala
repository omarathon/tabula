package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.tabula.coursework.web.controllers.BaseController
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.coursework.commands.feedback.FeedbackRecipientCheckCommand
import uk.ac.warwick.tabula.coursework.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.coursework.actions.Participate

/**
 * For calling by AJAX. Returns a report of the email addresses that would be
 * the recipients for any published feedback, noting ones that appear empty or
 * invalid.
 */
@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/check-recipients"))
class FeedbackRecipientCheckController extends BaseController {

	@RequestMapping()
	def confirmation(command: FeedbackRecipientCheckCommand, errors: Errors): Mav = {
		mustBeLinked(command.assignment, command.module)
		mustBeAbleTo(Participate(command.module))
		val report = command.apply()
		Mav("admin/assignments/publish/checkrecipients",
			"assignment" -> command.assignment,
			"report" -> report).noLayout()
	}

}