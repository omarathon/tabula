package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.feedback.AddFeedbackCommand
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/feedback/batch"))
class AddBatchFeedbackController extends CourseworkController {
	@ModelAttribute
	def command(@PathVariable assignment: Assignment, user: CurrentUser) =
		new AddFeedbackCommand(mandatory(assignment), user.apparentUser, user)

	// Add the common breadcrumbs to the model.
	def crumbed(mav: Mav, assignment: Assignment): Mav = mav.crumbsList(Breadcrumbs.assignment(assignment))

	@RequestMapping
	def uploadZipForm(@ModelAttribute cmd: AddFeedbackCommand, @PathVariable assignment: Assignment): Mav = {
		crumbed(Mav("cm2/admin/assignments/feedback/zipform"), assignment)
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmBatchUpload(@ModelAttribute cmd: AddFeedbackCommand, @PathVariable assignment: Assignment, errors: Errors): Mav = {
		cmd.preExtractValidation(errors)
		if (errors.hasErrors) {
			uploadZipForm(cmd, assignment)
		} else {
			cmd.postExtractValidation(errors)
			crumbed(Mav("cm2/admin/assignments/feedback/zipreview"), assignment)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(@ModelAttribute cmd: AddFeedbackCommand, errors: Errors, @PathVariable assignment: Assignment): Mav = {
		cmd.preExtractValidation(errors)
		cmd.postExtractValidation(errors)
		if (errors.hasErrors) {
			crumbed(Mav("cm2/admin/assignments/feedback/zipreview"), assignment)
		} else {
			// do apply, redirect back
			cmd.apply()
			Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))
		}
	}
}