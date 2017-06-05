package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.cm2.assignments.DeleteSubmissionsAndFeedbackCommand
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/submissionsandfeedback/delete"))
class DeleteSubmissionsAndFeedbackController extends CourseworkController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment): DeleteSubmissionsAndFeedbackCommand.Command =
		DeleteSubmissionsAndFeedbackCommand(assignment)

	@RequestMapping def redirectBackTo(@PathVariable assignment: Assignment) =
		Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(@ModelAttribute("command") command: DeleteSubmissionsAndFeedbackCommand.Command, @PathVariable assignment: Assignment): Mav =
		Mav("cm2/admin/assignments/submissionsandfeedback/delete")
			.crumbsList(Breadcrumbs.assignment(assignment))

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(@Valid @ModelAttribute("command") command: DeleteSubmissionsAndFeedbackCommand.Command, errors: Errors, @PathVariable assignment: Assignment): Mav =
		if (errors.hasErrors) showForm(command, assignment)
		else {
			command.apply()
			redirectBackTo(assignment)
		}

}
