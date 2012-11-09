package uk.ac.warwick.courses.web.controllers.admin

import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.courses.commands.assignments.AddFeedbackCommand
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.commands.feedback.DeleteFeedbackCommand
import uk.ac.warwick.courses.data.Transactions._
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.courses.actions.Participate
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.actions.Delete
import collection.JavaConversions._
import uk.ac.warwick.courses.web.Routes

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/feedback/delete"))
class DeleteFeedback extends BaseController {
	@ModelAttribute
	def command(@PathVariable assignment: Assignment) = new DeleteFeedbackCommand(assignment)

	validatesSelf[DeleteFeedbackCommand]

	def formView(assignment: Assignment) = Mav("admin/assignments/feedback/delete",
		"assignment" -> assignment)
		.crumbs(Breadcrumbs.Department(assignment.module.department), Breadcrumbs.Module(assignment.module))

	@RequestMapping(method = Array(GET))
	def get(@PathVariable assignment: Assignment) = Redirect(Routes.admin.assignment.feedback(assignment))

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(@PathVariable module: Module, @PathVariable assignment: Assignment,
		form: DeleteFeedbackCommand, errors: Errors) = {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Delete(mandatory(form.feedbacks.headOption)))
		form.prevalidate(errors)
		formView(assignment)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(
		@PathVariable module: Module, @PathVariable assignment: Assignment,
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