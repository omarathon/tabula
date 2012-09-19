package uk.ac.warwick.courses.web.controllers.admin

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.commands.assignments.AddFeedbackCommand
import javax.validation.Valid
import uk.ac.warwick.courses.ItemNotFoundException
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.courses.web.Mav
import org.springframework.stereotype.Controller
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.actions.Participate
import org.springframework.web.bind.annotation.RequestMethod._
import uk.ac.warwick.courses.web.Routes

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/new"))
class AddFeedbackController extends BaseController {

	@ModelAttribute
	def command(@PathVariable assignment: Assignment, user: CurrentUser) =
		new AddFeedbackCommand(assignment, user)

	def onBind(command: AddFeedbackCommand) {
		command.onBind
	}

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@PathVariable module: Module, @PathVariable assignment: Assignment,
		@ModelAttribute form: AddFeedbackCommand, errors: Errors) = {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		onBind(form)
		Mav("admin/assignments/feedback/form",
			"department" -> module.department,
			"module" -> module,
			"assignment" -> assignment)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

	@Transactional
	@RequestMapping(method = Array(POST))
	def submit(
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@Valid form: AddFeedbackCommand, errors: Errors) = {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		form.preExtractValidation(errors)
		onBind(form)
		form.postExtractValidation(errors)
		if (errors.hasErrors) {
			showForm(module, assignment, form, errors)
		} else {
			form.apply
			Mav("redirect:" + Routes.admin.module(module))
		}
	}

}