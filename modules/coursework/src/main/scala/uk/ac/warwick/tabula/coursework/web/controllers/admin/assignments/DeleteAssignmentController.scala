package uk.ac.warwick.tabula.coursework.web.controllers.admin.assignments

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._

import uk.ac.warwick.tabula.coursework.commands.assignments._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/delete"))
class DeleteAssignmentController extends CourseworkController {

	validatesSelf[DeleteAssignmentCommand]

	@ModelAttribute def formObject(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment) =
		new DeleteAssignmentCommand(module, mandatory(assignment))

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def showForm(form: DeleteAssignmentCommand) = {
		val (module, assignment) = (form.module, form.assignment)

		Mav("admin/assignments/delete",
			"department" -> module.department,
			"module" -> module,
			"assignment" -> assignment,
			"maxWordCount" -> Assignment.MaximumWordCount)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

	@RequestMapping(method = Array(RequestMethod.POST))
	def submit(@Valid form: DeleteAssignmentCommand, errors: Errors) = {
		if (errors.hasErrors) {
			showForm(form)
		} else {
			form.apply()
			Redirect(Routes.admin.module(form.module))
		}

	}

}
