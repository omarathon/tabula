package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.DeleteAssignmentCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController


@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/delete"))
class DeleteAssignmentController extends CourseworkController {

	validatesSelf[DeleteAssignmentCommand]

	@ModelAttribute
	def formObject(@PathVariable assignment: Assignment) =
		new DeleteAssignmentCommand(mandatory(assignment))

	def showForm(form: DeleteAssignmentCommand, assignment: Assignment): Mav = {
		Mav("cm2/admin/assignments/delete",
			"assignment" -> assignment
		).crumbs(Breadcrumbs.Department(assignment.module.adminDepartment, assignment.academicYear), Breadcrumbs.Assignment(assignment))
	}

	@RequestMapping(method = Array(GET))
	def deleteAssignment(form: DeleteAssignmentCommand, @PathVariable assignment: Assignment): Mav = {
		showForm(form, assignment)
	}

	@RequestMapping(method = Array(RequestMethod.POST))
	def deleteAssignment(@Valid form: DeleteAssignmentCommand, errors: Errors, @PathVariable assignment: Assignment): Mav = {
		if (errors.hasErrors) {
			showForm(form, assignment)
		} else {
			form.apply()
			Redirect(Routes.admin.department(assignment.module.adminDepartment, assignment.academicYear))
		}
	}
}
