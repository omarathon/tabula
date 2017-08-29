package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Assignment, AssignmentAnonymity}
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}"))
class ModifyAssignmentMarkersController extends AbstractAssignmentController {

	type ListMarkerAllocationsCommand = Appliable[MarkerAllocations] with ListMarkerAllocationsState
	type AssignMarkersCommand = Appliable[Assignment] with AssignMarkersState

	validatesSelf[SelfValidating]

	@ModelAttribute("assignMarkersCommand")
	def assignMarkersCommand(@PathVariable assignment: Assignment) = AssignMarkersCommand(mandatory(assignment))

	@ModelAttribute("listAllocationsCommand")
	def listAllocationsCommand(@PathVariable assignment: Assignment) = ListMarkerAllocationsCommand(mandatory(assignment))

	private def form(assignment: Assignment, listAllocationsCmd: ListMarkerAllocationsCommand, assignMarkersCmd: AssignMarkersCommand, mode: String): Mav = {
		val module = mandatory(assignMarkersCmd.assignment.module)
		val workflow = mandatory(assignMarkersCmd.assignment.cm2MarkingWorkflow)
		val existingAllocations = listAllocationsCmd.apply()

		val stages: Map[String, Seq[String]] = if (workflow.workflowType.rolesShareAllocations) {
			workflow.allStages.groupBy(_.roleName).mapValues(_.map(_.name))
		} else {
			workflow.allStages.map(s => s.allocationName -> Seq(s.name)).toMap
		}

		Mav("cm2/admin/assignments/assignment_assign_markers",
			"module" -> module,
			"department" -> module.adminDepartment,
			"stages" -> stages,
			"state" -> existingAllocations,
			"mode" -> mode,
			"AssignmentAnonymity" -> AssignmentAnonymity
		)
			.crumbsList(Breadcrumbs.assignment(assignment))
	}

	@RequestMapping(value = Array("new/markers"))
	def newForm(
		@PathVariable assignment: Assignment,
		@ModelAttribute("listAllocationsCommand") listAllocationsCmd: ListMarkerAllocationsCommand,
		@ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand
	): Mav = form(assignment, listAllocationsCmd, assignMarkersCmd, createMode)

	@RequestMapping(value = Array("edit/markers"))
	def editForm(
		@PathVariable assignment: Assignment,
		@ModelAttribute("listAllocationsCommand") listAllocationsCmd: ListMarkerAllocationsCommand,
		@ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand
	): Mav = form(assignment, listAllocationsCmd, assignMarkersCmd, editMode)

	def apply(assignment: Assignment, listAllocationsCmd: ListMarkerAllocationsCommand, assignMarkersCmd: AssignMarkersCommand, errors: Errors, redirect: String, mode:String): Mav = {
		if(errors.hasErrors) {
			form(assignment, listAllocationsCmd, assignMarkersCmd, mode)
		} else {
			assignMarkersCmd.apply()
			Redirect(redirect)
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddMarkers), value = Array("new/markers"))
	def saveAndExitCreate(
		@PathVariable assignment: Assignment,
		@ModelAttribute("listAllocationsCommand") listAllocationsCmd: ListMarkerAllocationsCommand,
		@Valid @ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand,
		errors: Errors
	): Mav = apply(assignment, listAllocationsCmd, assignMarkersCmd, errors, Routes.admin.assignment.submissionsandfeedback(assignment), createMode)

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddMarkers), value = Array("edit/markers"))
	def saveAndExitEdit(
		@PathVariable assignment: Assignment,
		@ModelAttribute("listAllocationsCommand") listAllocationsCmd: ListMarkerAllocationsCommand,
		@Valid @ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand,
		errors: Errors
	): Mav = apply(assignment, listAllocationsCmd, assignMarkersCmd, errors, Routes.admin.assignment.submissionsandfeedback(assignment), editMode)

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddSubmissions), value = Array("new/markers"))
	def submitAndAddSubmissionsCreate(
		@PathVariable assignment: Assignment,
		@ModelAttribute("listAllocationsCommand") listAllocationsCmd: ListMarkerAllocationsCommand,
		@Valid @ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand,
		errors: Errors
	): Mav = apply(assignment, listAllocationsCmd, assignMarkersCmd, errors, Routes.admin.assignment.createOrEditSubmissions(assignment, createMode), createMode)

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddSubmissions), value = Array("edit/markers"))
	def submitAndAddSubmissionsEdit(
		@PathVariable assignment: Assignment,
		@ModelAttribute("listAllocationsCommand") listAllocationsCmd: ListMarkerAllocationsCommand,
		@Valid @ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand,
		errors: Errors
	): Mav = apply(assignment, listAllocationsCmd, assignMarkersCmd, errors, Routes.admin.assignment.createOrEditSubmissions(assignment, editMode), editMode)

}
