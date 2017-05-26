package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments.{AssignMarkersBySpreadsheetCommand, AssignMarkersState, AssignMarkersTemplateCommand, AssignMarkersTemplateState}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.ExcelView

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}"))
class ModifyAssignmentMarkersSpreadsheetController extends AbstractAssignmentController {

	type AssignMarkersCommand = Appliable[Assignment] with AssignMarkersState
	type TemplateCommand = Appliable[ExcelView] with AssignMarkersTemplateState

	val assessmentMembershipService: AssessmentMembershipService = Wire.auto[AssessmentMembershipService]

	@ModelAttribute("assignMarkersBySpreadsheetCommand")
	def assignMarkersBySpreadsheetCommand(@PathVariable assignment: Assignment) =
		AssignMarkersBySpreadsheetCommand(mandatory(assignment))

	@ModelAttribute("templateCommand")
	def templateCommand(@PathVariable assignment: Assignment) = AssignMarkersTemplateCommand(mandatory(assignment))

	@RequestMapping(method = Array(GET, HEAD), value=Array("*/markers/template/download"))
	def downloadTemplate(@ModelAttribute("templateCommand") templateCommand: TemplateCommand): ExcelView = {
		templateCommand.apply()
	}

	private def showSpreadsheetForm(assignment: Assignment, assignMarkersBySpreadsheetCommand: AssignMarkersCommand, errors: Errors, mode: String): Mav = {
		val module =  mandatory(assignment.module)
		Mav("cm2/admin/assignments/assignment_markers_spreadsheet",
			"module" -> module,
			"department" -> module.adminDepartment,
			"fileTypes" -> AssignMarkersBySpreadsheetCommand.AcceptedFileExtensions,
			"mode" -> mode)
			.crumbsList(Breadcrumbs.assignment(assignment))
	}

	@RequestMapping(method = Array(GET, HEAD), value=Array("new/markers/template"))
	def createForm(
		@PathVariable assignment: Assignment,
		@ModelAttribute("assignMarkersBySpreadsheetCommand") assignMarkersBySpreadsheetCommand: AssignMarkersCommand,
		errors: Errors
	): Mav = showSpreadsheetForm(assignment, assignMarkersBySpreadsheetCommand, errors, createMode)

	@RequestMapping(method = Array(GET, HEAD),  value=Array("edit/markers/template"))
	def editForm(
		@PathVariable assignment: Assignment,
		@ModelAttribute("assignMarkersBySpreadsheetCommand") assignMarkersBySpreadsheetCommand: AssignMarkersCommand,
		errors: Errors
	): Mav = showSpreadsheetForm(assignment, assignMarkersBySpreadsheetCommand, errors, editMode)

	private def previewSpreadsheet(assignment: Assignment, assignMarkersBySpreadsheetCommand: AssignMarkersCommand, errors: Errors, mode: String): Mav = {
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

		Mav("cm2/admin/assignments/assignment_markers_spreadsheet_preview",
			"module" -> module,
			"department" -> module.adminDepartment,
			"allocationPreview" -> allocationPreview,
			"allocationOrder" -> workflow.allocationOrder,
			"unallocatedStudents" -> unallocatedStudents,
			"mode" -> mode)
			.crumbsList(Breadcrumbs.assignment(assignment))
	}

	@RequestMapping(method = Array(POST), value=Array("new/markers/template"), params = Array("preview"))
	def createPreview(
		@PathVariable assignment: Assignment,
		@Valid @ModelAttribute("assignMarkersBySpreadsheetCommand") assignMarkersBySpreadsheetCommand: AssignMarkersCommand,
		errors: Errors
	): Mav = previewSpreadsheet(assignment, assignMarkersBySpreadsheetCommand, errors, createMode)

	@RequestMapping(method = Array(POST), value=Array("edit/markers/template"), params = Array("preview"))
	def editPreview(
		@PathVariable assignment: Assignment,
		@Valid @ModelAttribute("assignMarkersBySpreadsheetCommand") assignMarkersBySpreadsheetCommand: AssignMarkersCommand,
		errors: Errors
	): Mav = previewSpreadsheet(assignment, assignMarkersBySpreadsheetCommand, errors, editMode)

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddMarkers), value = Array("*/markers/template"))
	def saveAndExit(
		@PathVariable assignment: Assignment,
		@Valid @ModelAttribute("assignMarkersBySpreadsheetCommand") assignMarkersBySpreadsheetCommand: AssignMarkersCommand
	): Mav =  {
		assignMarkersBySpreadsheetCommand.apply()
		Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddSubmissions), value = Array("new/markers/template"))
	def submitAndAddSubmissionsCreate(
		@PathVariable assignment: Assignment,
		@Valid @ModelAttribute("assignMarkersBySpreadsheetCommand") assignMarkersBySpreadsheetCommand: AssignMarkersCommand
	): Mav = {
		val assignment = assignMarkersBySpreadsheetCommand.apply()
		RedirectForce(Routes.admin.assignment.createOrEditSubmissions(assignment, createMode))
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddSubmissions), value = Array("edit/markers/template"))
	def submitAndAddSubmissionsEdit(
		@PathVariable assignment: Assignment,
		@Valid @ModelAttribute("assignMarkersBySpreadsheetCommand") assignMarkersBySpreadsheetCommand: AssignMarkersCommand
	): Mav = {
		val assignment = assignMarkersBySpreadsheetCommand.apply()
		RedirectForce(Routes.admin.assignment.createOrEditSubmissions(assignment, editMode))
	}

}
