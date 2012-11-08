package uk.ac.warwick.courses.web.controllers.admin

import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.commands.assignments.DeleteSubmissionCommand
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.courses.actions.Participate
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.actions.Delete
import collection.JavaConversions._
import uk.ac.warwick.courses.web.Routes
import uk.ac.warwick.courses.commands.assignments.DeleteSubmissionCommand
import uk.ac.warwick.courses.commands.assignments.MarkPlagiarisedCommand
import uk.ac.warwick.courses.data.Transactions._

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions/mark-plagiarised"))
class MarkPlagiarisedController extends BaseController {
	@ModelAttribute
	def command(@PathVariable assignment: Assignment) = new MarkPlagiarisedCommand(assignment)

	validatesSelf[MarkPlagiarisedCommand]

	def formView(assignment: Assignment) = Mav("admin/assignments/submissions/mark-plagiarised",
		"assignment" -> assignment)
		.crumbs(Breadcrumbs.Department(assignment.module.department), Breadcrumbs.Module(assignment.module))

	def RedirectBack(assignment: Assignment) = Redirect(Routes.admin.assignment.submission(assignment))

	// shouldn't ever be called as a GET - if it is, just redirect back to the submission list
	@RequestMapping(method = Array(GET))
	def get(@PathVariable assignment: Assignment) = RedirectBack(assignment)

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(
			@PathVariable module: Module, 
			@PathVariable assignment: Assignment,
			form: MarkPlagiarisedCommand, errors: Errors) = {
		mustBeLinked(assignment, module)
		mustBeAbleTo(Participate(module))
		form.prevalidate(errors)
		formView(assignment)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(
			@PathVariable module: Module,
			@PathVariable assignment: Assignment,
			@Valid form: MarkPlagiarisedCommand, errors: Errors) = {
		transactional() {
			mustBeLinked(assignment, module)
			mustBeAbleTo(Participate(module))
			if (errors.hasErrors) {
				formView(assignment)
			} else {
				form.apply()
				RedirectBack(assignment)
			}
		}
	}
}