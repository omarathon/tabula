package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.coursework.commands.assignments.AddFeedbackCommand
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.coursework.commands.feedback.DeleteFeedbackCommand
import uk.ac.warwick.tabula.data.Transactions._
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.permissions._
import collection.JavaConversions._
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/delete"))
class DeleteFeedback extends CourseworkController {
	@ModelAttribute
	def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment) = 
		new DeleteFeedbackCommand(module, assignment)

	validatesSelf[DeleteFeedbackCommand]

	def formView(assignment: Assignment) = Mav("admin/assignments/feedback/delete",
		"assignment" -> assignment)
		.crumbs(Breadcrumbs.Department(assignment.module.department), Breadcrumbs.Module(assignment.module))

	@RequestMapping(method = Array(GET))
	def get(form: DeleteFeedbackCommand) = Redirect(Routes.admin.assignment.feedback(form.assignment))

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(form: DeleteFeedbackCommand, errors: Errors) = {
		form.prevalidate(errors)
		formView(form.assignment)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(@Valid form: DeleteFeedbackCommand, errors: Errors) = {
		transactional() {
			if (errors.hasErrors) {
				formView(form.assignment)
			} else {
				form.apply()
				Redirect(Routes.admin.assignment.feedback(form.assignment))
			}
		}
	}
}