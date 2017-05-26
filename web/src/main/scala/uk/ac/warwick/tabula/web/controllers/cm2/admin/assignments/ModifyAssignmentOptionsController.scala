package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.{ModifyAssignmentOptionsCommand, _}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.turnitinlti.TurnitinLtiService
import uk.ac.warwick.tabula.web.Mav

abstract class AbstractAssignmentOptionsController extends AbstractAssignmentController {

	type ModifyAssignmentOptionsCommand = Appliable[Assignment] with ModifyAssignmentOptionsCommandState with PopulateOnForm

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def modifyAssignmentFeedbackCommand(@PathVariable assignment: Assignment) =
		ModifyAssignmentOptionsCommand(mandatory(assignment))

	def showForm(form: ModifyAssignmentOptionsCommand, assignment: Assignment, mode: String): Mav = {
		val module = form.assignment.module
		Mav("cm2/admin/assignments/assignment_options_details",
			"module" -> module,
			"mode" -> mode,
			"turnitinFileSizeLimit" -> TurnitinLtiService.maxFileSizeInMegabytes)
			.crumbsList(Breadcrumbs.assignment(assignment))
	}

	def submit(cmd: ModifyAssignmentOptionsCommand, errors: Errors, assignment: Assignment, mav: Mav, mode: String): Mav = {
		if (errors.hasErrors) {
			showForm(cmd, assignment, mode)
		} else {
			cmd.apply()
			mav
		}
	}

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}"))
class ModifyAssignmentOptionsController extends AbstractAssignmentOptionsController {

	@ModelAttribute("command") def command(@PathVariable assignment: Assignment): ModifyAssignmentOptionsCommand =
		ModifyAssignmentOptionsCommand(mandatory(assignment))


	@RequestMapping(method = Array(GET), value = Array("/new/options"))
	def form(
		@PathVariable assignment: Assignment,
		@ModelAttribute("command") cmd: ModifyAssignmentOptionsCommand
	): Mav = {
		cmd.populate()
		showForm(cmd, assignment, createMode)
	}

	@RequestMapping(method = Array(GET), value = Array("/edit/options"))
	def formEdit(
		@PathVariable assignment: Assignment,
		@ModelAttribute("command") cmd: ModifyAssignmentOptionsCommand
	): Mav = {
		cmd.populate()
		showForm(cmd, assignment, editMode)
	}

	@RequestMapping(method = Array(POST), value = Array("/new/options"), params = Array(ManageAssignmentMappingParameters.reviewAssignment, "action!=refresh", "action!=update"))
	def submitAndAddOptions(@Valid @ModelAttribute("command") cmd: ModifyAssignmentOptionsCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
		submit(cmd, errors, assignment, RedirectForce(Routes.admin.assignment.reviewAssignment(assignment)), createMode)

	@RequestMapping(method = Array(POST), value = Array("/new/options"), params = Array(ManageAssignmentMappingParameters.createAndAddOptions, "action!=refresh", "action!=update"))
	def saveAndExit(@Valid @ModelAttribute("command") cmd: ModifyAssignmentOptionsCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
		submit(cmd, errors, assignment, Redirect(Routes.admin.moduleWithinDepartment(assignment.module, assignment.academicYear)), createMode)

	@RequestMapping(method = Array(POST), value = Array("/edit/options"), params = Array(ManageAssignmentMappingParameters.reviewAssignment, "action!=refresh", "action!=update"))
	def submitAndAddOptionsForEdit(@Valid @ModelAttribute("command") cmd: ModifyAssignmentOptionsCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
		submit(cmd, errors, assignment, RedirectForce(Routes.admin.assignment.reviewAssignment(assignment)), editMode)

	@RequestMapping(method = Array(POST), value = Array("/edit/options"), params = Array(ManageAssignmentMappingParameters.editAndAddOptions, "action!=refresh", "action!=update"))
	def saveAndExitForEdit(@Valid @ModelAttribute("command") cmd: ModifyAssignmentOptionsCommand, errors: Errors, @PathVariable assignment: Assignment): Mav =
		submit(cmd, errors, assignment, Redirect(Routes.admin.moduleWithinDepartment(assignment.module, assignment.academicYear)), editMode)

}