package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.{CourseworkBreadcrumbs, CourseworkController}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/new/{assignment}/markers/smallgroups"))
class ModifyAssignmentMarkersSmallGroupsController extends CourseworkController {

	type AssignMarkersCommand = Appliable[Assignment] with AssignMarkersState
	type SmallGroupCommand = Appliable[Seq[SetAllocation]] with AssignMarkersSmallGroupsState

	@ModelAttribute("ManageAssignmentMappingParameters")
	def params = ManageAssignmentMappingParameters

	@ModelAttribute("assignMarkersCommand")
	def assignMarkersCommand(@PathVariable assignment: Assignment) = AssignMarkersCommand(mandatory(assignment))

	@ModelAttribute("smallGroupCommand")
	def smallGroupCommand(@PathVariable assignment: Assignment) = AssignMarkersSmallGroupsCommand(mandatory(assignment))

	@RequestMapping(method = Array(GET, HEAD))
	def form(
		@PathVariable("assignment") assignment: Assignment,
		@ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand,
		@ModelAttribute("smallGroupCommand") smallGroupCommand: SmallGroupCommand
	): Mav = {
		val module =  mandatory(assignment.module)
		val allocations = smallGroupCommand.apply()
		val workflow = assignment.cm2MarkingWorkflow

		Mav(s"$urlPrefix/admin/assignments/assignment_markers_smallgroups",
			"module" -> module,
			"sets" -> allocations.map(_.set),
			"allocations" -> allocations,
			"allocationOrder" -> workflow.allocationOrder,
			"stageNames" -> workflow.allStages.groupBy(_.roleName).mapValues(_.map(_.name))
		).crumbs(CourseworkBreadcrumbs.Assignment.AssignmentManagement())
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddMarkers))
	def saveAndExit(
		@ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand
	): Mav =  {
		assignMarkersCmd.apply()
		RedirectForce(Routes.home)
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddSubmissions))
	def submitAndAddSubmissions(
		@Valid @ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand
	): Mav = {
		val assignment = assignMarkersCmd.apply()
		RedirectForce(Routes.admin.assignment.createAddSubmissions(assignment))
	}

}
