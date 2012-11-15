package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.coursework.commands.assignments.DeleteSubmissionCommand
import uk.ac.warwick.tabula.data.Transactions._
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.actions.Participate
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.actions.Delete
import collection.JavaConversions._
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.commands.assignments.DeleteSubmissionCommand

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/submissions/delete"))
class DeleteSubmission extends CourseworkController {

	validatesSelf[DeleteSubmissionCommand]
	
	@ModelAttribute
	def command(@PathVariable("assignment") assignment: Assignment) = new DeleteSubmissionCommand(assignment)
	
	@RequestMapping(method = Array(GET))
	def get(form: DeleteSubmissionCommand) = RedirectBack(form.assignment)

	@RequestMapping(method = Array(POST), params = Array("!confirmScreen"))
	def showForm(@PathVariable("module") module: Module, form: DeleteSubmissionCommand, errors: Errors) = {
		val assignment = form.assignment
		mustBeLinked(assignment, module)
		mustBeAbleTo(Delete(mandatory(form.submissions.headOption)))
		form.prevalidate(errors)
		formView(assignment)
	}

	@RequestMapping(method = Array(POST), params = Array("confirmScreen"))
	def submit(@PathVariable("module") module: Module, @Valid form: DeleteSubmissionCommand,errors: Errors) = {
		transactional() {
			val assignment = form.assignment
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
	
	
	def formView(assignment: Assignment) = 
		Mav("admin/assignments/submissions/delete",
			"assignment" -> assignment)
			.crumbs(
					Breadcrumbs.Department(assignment.module.department), 
					Breadcrumbs.Module(assignment.module))

	def RedirectBack(assignment: Assignment) = Redirect(Routes.admin.assignment.submission(assignment))

}