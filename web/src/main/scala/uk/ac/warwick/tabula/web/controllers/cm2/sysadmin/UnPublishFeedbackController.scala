package uk.ac.warwick.tabula.web.controllers.cm2.sysadmin

import javax.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.cm2.feedback.UnPublishFeedbackCommand
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/unpublish"))
class UnPublishFeedbackController extends CourseworkController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment, user: CurrentUser): UnPublishFeedbackCommand.Command = UnPublishFeedbackCommand(mandatory(assignment), user)

	@RequestMapping(params = Array("!confirm"))
	def confirmationPage(@ModelAttribute("command") command: UnPublishFeedbackCommand.Command, @PathVariable assignment: Assignment): Mav =
		Mav("cm2/admin/assignments/unpublish/form").crumbsList(Breadcrumbs.assignment(assignment))

	@RequestMapping(params = Array("confirm"))
	def publish(@Valid @ModelAttribute("command") command: UnPublishFeedbackCommand.Command, errors: Errors, @PathVariable assignment: Assignment): Mav =
		if (errors.hasErrors) confirmationPage(command, assignment)
		else {
			command.apply()
			Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))
		}

}