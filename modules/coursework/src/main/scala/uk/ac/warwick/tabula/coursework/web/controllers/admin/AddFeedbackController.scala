package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.commands.assignments.AddFeedbackCommand
import javax.validation.Valid
import uk.ac.warwick.tabula.ItemNotFoundException
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.web.Mav
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.actions.Participate
import org.springframework.web.bind.annotation.RequestMethod._
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/new"))
class AddFeedbackController extends CourseworkController {

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

	@RequestMapping(method = Array(POST))
	def submit(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@Valid form: AddFeedbackCommand, errors: Errors) = {
		transactional() {
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

}