package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.tabula.coursework.web.controllers.BaseController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.coursework.commands.assignments.AddFeedbackCommand
import uk.ac.warwick.tabula.coursework.data.model.Assignment
import uk.ac.warwick.tabula.coursework.commands.feedback.DeleteFeedbackCommand
import uk.ac.warwick.tabula.data.Transactions._
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.coursework.actions.Participate
import uk.ac.warwick.tabula.coursework.data.model.Module
import uk.ac.warwick.tabula.coursework.actions.Delete
import collection.JavaConversions._
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/delete"))
class DeleteFeedback extends BaseController {
	@ModelAttribute
	def command(@PathVariable("assignment") assignment: Assignment) = new DeleteFeedbackCommand(assignment)

	validatesSelf[DeleteFeedbackCommand]

	def formView(assignment: Assignment) = Mav("admin/assignments/feedback/delete",
		"assignment" -> assignment)
		.crumbs(Breadcrumbs.Department(assignment.module.department), Breadcrumbs.Module(assignment.module))

	@RequestMapping(method = Array(GET))
	def get(@PathVariable("assignment") assignment: Assignment) = Redirect(Routes.admin.assignment.feedback(assignment))

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment,
		form: DeleteFeedbackCommand, errors: Errors) = {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Delete(mandatory(form.feedbacks.headOption)))
		form.prevalidate(errors)
		formView(assignment)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(
		@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment,
		@Valid form: DeleteFeedbackCommand, errors: Errors) = {
		transactional() {
			mustBeLinked(assignment, module)
			mustBeAbleTo(Participate(module))
			if (errors.hasErrors) {
				formView(assignment)
			} else {
				form.apply()
				Redirect(Routes.admin.assignment.feedback(assignment))
			}
		}
	}
}