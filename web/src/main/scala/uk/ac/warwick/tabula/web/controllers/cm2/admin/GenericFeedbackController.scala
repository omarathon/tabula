package uk.ac.warwick.tabula.web.controllers.cm2.admin

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.feedback.GenericFeedbackCommand
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/assignments/{assignment}/feedback/generic"))
class GenericFeedbackController extends CourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment) =
		GenericFeedbackCommand(mandatory(assignment))

	@RequestMapping
	def showForm(
		@PathVariable assignment: Assignment,
		@ModelAttribute("command") command: GenericFeedbackCommand,
		errors: Errors
	): Mav = {

		Mav("cm2/admin/assignments/feedback/generic_feedback", "ajax" -> ajax).noLayoutIf(ajax)
		  .crumbsList(Breadcrumbs.assignment(assignment))
	}


	@RequestMapping(method = Array(POST))
	def submit(
		@PathVariable assignment: Assignment,
		@ModelAttribute("command") @Valid command: GenericFeedbackCommand,
		errors: Errors
	): Mav = {
		command.apply()
		if (ajax)
			Mav("ajax_success")
		else
			Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))
	}
}