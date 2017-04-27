package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.{CourseworkBreadcrumbs, CourseworkController}
import uk.ac.warwick.tabula.web.views.ExcelView

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/new/{assignment}/markers"))
class ModifyAssignmentMarkersController extends CourseworkController {

	type ListMarkerAllocationsCommand = Appliable[MarkerAllocations] with ListMarkerAllocationsState
	type AssignMarkersCommand = Appliable[Assignment] with AssignMarkersState
	type TemplateCommand = Appliable[ExcelView] with AssignMarkersTemplateState

	validatesSelf[SelfValidating]

	val assessmentMembershipService: AssessmentMembershipService = Wire.auto[AssessmentMembershipService]

	@ModelAttribute("assignMarkersCommand")
	def assignMarkersCommand(@PathVariable assignment: Assignment) = AssignMarkersCommand(mandatory(assignment))

	@ModelAttribute("assignMarkersBySpreadsheetCommand")
	def assignMarkersBySpreadsheetCommand(@PathVariable assignment: Assignment) =
		AssignMarkersBySpreadsheetCommand(mandatory(assignment))

	@ModelAttribute("listAllocationsCommand")
	def listAllocationsCommand(@PathVariable assignment: Assignment) = ListMarkerAllocationsCommand(mandatory(assignment))

	@ModelAttribute("templateCommand")
	def templateCommand(@PathVariable assignment: Assignment) = AssignMarkersTemplateCommand(mandatory(assignment))

	@ModelAttribute("ManageAssignmentMappingParameters")
	def params = ManageAssignmentMappingParameters

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("assignment") assignment: Assignment,
		@ModelAttribute("listAllocationsCommand") listAllocationsCmd: ListMarkerAllocationsCommand,
		@ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand
	): Mav = {
		val module =  mandatory(assignMarkersCmd.assignment.module)
		val workflow = mandatory(assignMarkersCmd.assignment.cm2MarkingWorkflow)
		val existingAllocations = listAllocationsCmd.apply()

		Mav(s"$urlPrefix/admin/assignments/assignment_assign_markers",
			"module" -> module,
			"department" -> module.adminDepartment,
			"stages" -> workflow.allStages.groupBy(_.roleName),
			"state" -> existingAllocations
		).crumbs(CourseworkBreadcrumbs.Assignment.AssignmentManagement())
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddMarkers))
	def saveAndExit(
		@ModelAttribute("listAllocationsCommand") listAllocationsCmd: ListMarkerAllocationsCommand,
		@ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand
	): Mav =  {
		assignMarkersCmd.apply()
		RedirectForce(Routes.home)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddSubmissions))
	def submitAndAddSubmissions(
		@ModelAttribute("listAllocationsCommand") listAllocationsCmd: ListMarkerAllocationsCommand,
		@Valid @ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand
	): Mav = {
		val assignment = assignMarkersCmd.apply()
		RedirectForce(Routes.admin.assignment.createAddSubmissions(assignment))
	}

	@RequestMapping(method = Array(GET, HEAD), value=Array("template"))
	def showSpreadsheetForm(
		@PathVariable("assignment") assignment: Assignment,
		@ModelAttribute("assignMarkersBySpreadsheetCommand") assignMarkersBySpreadsheetCommand: AssignMarkersCommand,
	  errors: Errors
	): Mav = {
		val module =  mandatory(assignment.module)
		Mav(s"$urlPrefix/admin/assignments/assignment_markers_spreadsheet",
			"module" -> module,
			"department" -> module.adminDepartment,
			"fileTypes" -> AssignMarkersBySpreadsheetCommand.AcceptedFileExtensions
		).crumbs(CourseworkBreadcrumbs.Assignment.AssignmentManagement())
	}

	@RequestMapping(method = Array(GET, HEAD), value=Array("template/download"))
	def downloadTemplate(@ModelAttribute("templateCommand") templateCommand: TemplateCommand): ExcelView = {
		templateCommand.apply()
	}

	@RequestMapping(method = Array(POST), value=Array("template"), params = Array("preview"))
	def previewSpreadsheet(
		@PathVariable("assignment") assignment: Assignment,
		@Valid @ModelAttribute("assignMarkersBySpreadsheetCommand") assignMarkersBySpreadsheetCommand: AssignMarkersCommand,
		errors: Errors
	): Mav = {
		val module =  mandatory(assignment.module)
		val workflow = assignment.cm2MarkingWorkflow
		val allocationPreview = if(workflow.workflowType.rolesShareAllocations){
			assignMarkersBySpreadsheetCommand.allocationMap
				.groupBy{case (s, _) => s.roleName}
				.map{case (roleName, allocationsMap) => roleName -> allocationsMap.values.head}
		} else {
			assignMarkersBySpreadsheetCommand.allocationMap.map{case (stage, allocations) => stage.allocationName -> allocations}
		}

		val allStudents = assessmentMembershipService.determineMembershipUsers(assignment).toSet
		val unallocatedStudents = allocationPreview.map{ case(key, allocation) =>
			key -> (allStudents -- allocation.filterKeys(_.isFoundUser).values.flatten.toSet)
		}

		Mav(s"$urlPrefix/admin/assignments/assignment_markers_spreadsheet_preview",
			"module" -> module,
			"department" -> module.adminDepartment,
			"allocationPreview" -> allocationPreview,
			"allocationOrder" -> workflow.allocationOrder,
			"unallocatedStudents" -> unallocatedStudents
		).crumbs(CourseworkBreadcrumbs.Assignment.AssignmentManagement())
	}

	@RequestMapping(method = Array(POST), value=Array("template"), params = Array(ManageAssignmentMappingParameters.createAndAddMarkers))
	def spreadsheetAndExit(
		@PathVariable("assignment") assignment: Assignment,
		@Valid @ModelAttribute("assignMarkersBySpreadsheetCommand") assignMarkersBySpreadsheetCommand: AssignMarkersCommand,
		errors: Errors
	): Mav = {
		assignMarkersBySpreadsheetCommand.apply()
		RedirectForce(Routes.home)
	}

	@RequestMapping(method = Array(POST), value=Array("template"), params = Array(ManageAssignmentMappingParameters.createAndAddSubmissions))
	def spreadsheetAndAddSubmissions(
		@PathVariable("assignment") assignment: Assignment,
		@Valid @ModelAttribute("assignMarkersBySpreadsheetCommand") assignMarkersBySpreadsheetCommand: AssignMarkersCommand,
		errors: Errors
	): Mav = {
		assignMarkersBySpreadsheetCommand.apply()
		RedirectForce(Routes.admin.assignment.createAddSubmissions(assignment))
	}

}
