package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.{CourseworkBreadcrumbs, CourseworkController}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/new/{assignment}/markers"))
class ModifyAssignmentMarkersController extends CourseworkController {

	type ListMarkerAllocationsCommand = Appliable[MarkerAllocations] with ListMarkerAllocationsState
	type AssignMarkersCommand = Appliable[Assignment] with AssignMarkersState

	validatesSelf[SelfValidating]

	@ModelAttribute("assignMarkersCommand")
	def assignMarkersCommand(@PathVariable assignment: Assignment) = AssignMarkersCommand(mandatory(assignment))

	@ModelAttribute("listAllocationsCommand")
	def listAllocationsCommand(@PathVariable assignment: Assignment) = ListMarkerAllocationsCommand(mandatory(assignment))

	@ModelAttribute("ManageAssignmentMappingParameters")
	def params = ManageAssignmentMappingParameters

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("assignment") assignment: Assignment,
		@ModelAttribute("listAllocationsCommand") listAllocationsCmd: ListMarkerAllocationsCommand,
		@ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand
	): Mav = {
		showForm(listAllocationsCmd, assignMarkersCmd)
	}

	def showForm(list: ListMarkerAllocationsCommand, form: AssignMarkersCommand): Mav = {
		val module =  mandatory(form.assignment.module)
		val workflow = mandatory(form.assignment.cm2MarkingWorkflow)
		val existingAllocations = list.apply()

		Mav(s"$urlPrefix/admin/assignments/assignment_assign_markers",
			"module" -> module,
			"department" -> module.adminDepartment,
			"stages" -> workflow.allStages.groupBy(_.roleName),
			"state" -> existingAllocations
		).crumbs(CourseworkBreadcrumbs.Assignment.AssignmentManagement())
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddMarkers, "action!=refresh", "action!=update"))
	def saveAndExit(
		@ModelAttribute("listAllocationsCommand") listAllocationsCmd: ListMarkerAllocationsCommand,
		@ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand,
		errors: Errors
	): Mav =  submit(listAllocationsCmd, assignMarkersCmd, errors, { _ => Routes.home })

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddSubmissions, "action!=refresh", "action!=update"))
	def submitAndAddSubmissions(
		@ModelAttribute("listAllocationsCommand") listAllocationsCmd: ListMarkerAllocationsCommand,
		@Valid @ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand,
		errors: Errors
	): Mav = submit(listAllocationsCmd, assignMarkersCmd, errors, Routes.admin.assignment.createAddSubmissions)

	private def submit(
		list: ListMarkerAllocationsCommand,
		assignMarkersCmd: AssignMarkersCommand,
		errors: Errors,
		route: Assignment => String
	) = {
		if (errors.hasErrors) {
			showForm(list, assignMarkersCmd)
		} else {
			val assignment = assignMarkersCmd.apply()
			RedirectForce(route(assignment))
		}
	}

}
