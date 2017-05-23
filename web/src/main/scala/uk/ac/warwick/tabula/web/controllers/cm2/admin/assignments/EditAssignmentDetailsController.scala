package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.{EditAssignmentDetailsCommand, _}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/edit"))
class EditAssignmentDetailsController extends AbstractAssignmentController {

	type EditAssignmentDetailsCommand = Appliable[Assignment] with EditAssignmentDetailsCommandState with PopulateOnForm
	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def createAssignmentDetailsCommand(@PathVariable assignment: Assignment) =
		EditAssignmentDetailsCommand(mandatory(assignment))

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: EditAssignmentDetailsCommand, @PathVariable assignment: Assignment): Mav = {
		cmd.populate()
		showForm(cmd, assignment)
	}

	def showForm(cmd: EditAssignmentDetailsCommand, @PathVariable assignment: Assignment): Mav = {
		val module = assignment.module
		val canDeleteAssignment = !assignment.deleted  && assignment.submissions.isEmpty && !assignment.hasReleasedFeedback
		Mav("cm2/admin/assignments/edit_assignment_details",
			"department" -> module.adminDepartment,
			"module" -> module,
			"academicYear" -> cmd.academicYear,
			"reusableWorkflows" -> cmd.availableWorkflows,
			"workflow" -> cmd.workflow,
			"canDeleteMarkers" -> cmd.workflow.canDeleteMarkers,
			"canDeleteAssignment" -> canDeleteAssignment)
			.crumbs(Breadcrumbs.Department(assignment.module.adminDepartment, assignment.academicYear), Breadcrumbs.Assignment(assignment))
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.editAndAddFeedback, "action!=refresh", "action!=update, action=submit"))
	def submitAndAddFeedback(@Valid @ModelAttribute("command") cmd: EditAssignmentDetailsCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
		submit(cmd, errors, assignment, Routes.admin.assignment.createOrEditFeedback(assignment, editMode))

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.editAndEditDetails, "action!=refresh", "action!=update"))
	def saveAndExit(@ModelAttribute("command") cmd: EditAssignmentDetailsCommand, errors: Errors, @PathVariable assignment: Assignment): Mav = {
		submit(cmd, errors, assignment, Routes.home)
	}

	private def submit(cmd: EditAssignmentDetailsCommand, errors: Errors, assignment: Assignment, path: String) = {
		if (errors.hasErrors) showForm(cmd, assignment)
		else {
			cmd.apply()
			RedirectForce(path)
		}
	}
}
