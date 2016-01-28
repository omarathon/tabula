package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.coursework.assignments.AddFeedbackCommand
import javax.validation.Valid
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/new"))
class AddFeedbackController extends CourseworkController {

	@ModelAttribute
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment, user: CurrentUser) =
		new AddFeedbackCommand(module, assignment, user.apparentUser, user)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute form: AddFeedbackCommand) = {
		Mav("admin/assignments/feedback/form",
			"department" -> form.module.adminDepartment,
			"module" -> form.module,
			"assignment" -> form.assignment)
			.crumbs(Breadcrumbs.Department(form.module.adminDepartment), Breadcrumbs.Module(form.module))
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid form: AddFeedbackCommand, errors: Errors) = {
		transactional() {
			form.preExtractValidation(errors)
			form.postExtractValidation(errors)
			if (errors.hasErrors) {
				showForm(form)
			} else {
				form.apply()
				Mav("redirect:" + Routes.admin.module(form.module))
			}
		}
	}

}