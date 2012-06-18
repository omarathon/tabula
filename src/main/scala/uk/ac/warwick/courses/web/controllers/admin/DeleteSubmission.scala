package uk.ac.warwick.courses.web.controllers.admin

import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.commands.assignments.DeleteSubmissionCommand
import org.springframework.transaction.annotation.Transactional
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.courses.actions.Participate
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.actions.Delete
import collection.JavaConversions._
import uk.ac.warwick.courses.web.Routes
import uk.ac.warwick.courses.commands.assignments.DeleteSubmissionCommand

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/submissions/delete"))
class DeleteSubmission extends BaseController {
	@ModelAttribute
	def command(@PathVariable assignment:Assignment) = new DeleteSubmissionCommand(assignment)
	
	validatesSelf[DeleteSubmissionCommand]
	
	def formView(assignment:Assignment) = Mav("admin/assignments/submissions/delete",
				"assignment" -> assignment)
				.crumbs(Breadcrumbs.Department(assignment.module.department), Breadcrumbs.Module(assignment.module))
	
	def RedirectBack(assignment:Assignment) = Redirect(Routes.admin.assignment.submission(assignment))

	@RequestMapping(method=Array(GET))
	def get(@PathVariable assignment:Assignment) = RedirectBack(assignment)
	
	
	@RequestMapping(method=Array(POST), params=Array("!confirmScreen"))
	def showForm(@PathVariable module:Module, @PathVariable assignment:Assignment, 
			form:DeleteSubmissionCommand, errors: Errors) = {
		mustBeLinked(assignment,module)
		mustBeAbleTo(Delete(mandatory(form.submissions.headOption)))
		form.prevalidate(errors)
		formView(assignment)
	}
	
	@Transactional
	@RequestMapping(method = Array(POST), params=Array("confirmScreen"))
	def submit(
			@PathVariable module: Module, 
			@PathVariable assignment: Assignment,
			@Valid form: DeleteSubmissionCommand, 
			errors: Errors) = {
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))
		if (errors.hasErrors) {
			formView(assignment)
		} else {
			form.apply()
			RedirectBack(assignment)
		}
	}
}