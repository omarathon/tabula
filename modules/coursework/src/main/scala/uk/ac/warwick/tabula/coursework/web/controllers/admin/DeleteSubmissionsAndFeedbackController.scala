package uk.ac.warwick.tabula.coursework.web.controllers.admin
import scala.collection.JavaConversions._

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable

import org.springframework.web.bind.annotation.RequestMapping

import javax.validation.Valid
import uk.ac.warwick.tabula.coursework.commands.assignments.DeleteSubmissionsAndFeedbackCommand
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissionsandfeedback/delete"))
class DeleteSubmissionsAndFeedback extends CourseworkController {

	@ModelAttribute
	def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment) = new DeleteSubmissionsAndFeedbackCommand(module, assignment)

	validatesSelf[DeleteSubmissionsAndFeedbackCommand]

	def formView(assignment: Assignment) =
		Mav("admin/assignments/submissionsandfeedback/delete",
			"assignment" -> assignment)
			.crumbs(
					Breadcrumbs.Department(assignment.module.department), 
					Breadcrumbs.Module(assignment.module))

	def RedirectBack(assignment: Assignment) = Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))

	@RequestMapping(method = Array(GET))
	def get(form: DeleteSubmissionsAndFeedbackCommand) = RedirectBack(form.assignment)

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(form: DeleteSubmissionsAndFeedbackCommand, errors: Errors) = {
		form.prevalidate(errors)
		formView(form.assignment)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(
			@Valid form: DeleteSubmissionsAndFeedbackCommand,
			errors: Errors) = {
		transactional() {
			if (errors.hasErrors) {
				formView(form.assignment)
			} else {
				form.apply()
				RedirectBack(form.assignment)
			}
		}
	}
}
