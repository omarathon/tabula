package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments


import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}"))
class ModifyAssignmentMarkersSmallGroupsController extends AbstractAssignmentController {

	type AssignMarkersCommand = Appliable[Assignment] with AssignMarkersState
	type SmallGroupCommand = Appliable[Seq[SetAllocation]] with AssignMarkersSmallGroupsState

	@ModelAttribute("assignMarkersCommand")
	def assignMarkersCommand(@PathVariable assignment: Assignment) = AssignMarkersCommand(mustBeCM2(mandatory(assignment)))

	@ModelAttribute("smallGroupCommand")
	def smallGroupCommand(@PathVariable assignment: Assignment) = AssignMarkersSmallGroupsCommand(mustBeCM2(mandatory(assignment)))

	private def form(assignment: Assignment, assignMarkersCmd: AssignMarkersCommand,  smallGroupCommand: SmallGroupCommand, mode:String): Mav = {
		val module =  mandatory(assignment.module)
		val allocations = smallGroupCommand.apply()
		val workflow = assignment.cm2MarkingWorkflow

		Mav("cm2/admin/assignments/assignment_markers_smallgroups",
			"module" -> module,
			"sets" -> allocations.map(_.set),
			"allocations" -> allocations,
			"allocationOrder" -> workflow.allocationOrder,
			"stageNames" -> workflow.allStages.groupBy(_.roleName).mapValues(_.map(_.name)),
			"mode" -> mode)
			.crumbsList(Breadcrumbs.assignment(assignment))
	}

	@RequestMapping(method = Array(GET, HEAD), value = Array("new/markers/smallgroups"))
	def createForm(
		@PathVariable assignment: Assignment,
		@ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand,
		@ModelAttribute("smallGroupCommand") smallGroupCommand: SmallGroupCommand
	): Mav = form(assignment, assignMarkersCmd, smallGroupCommand, createMode)

	@RequestMapping(method = Array(GET, HEAD), value = Array("edit/markers/smallgroups"))
	def editForm(
		@PathVariable assignment: Assignment,
		@ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand,
		@ModelAttribute("smallGroupCommand") smallGroupCommand: SmallGroupCommand
	): Mav = form(assignment, assignMarkersCmd, smallGroupCommand, editMode)

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddMarkers), value = Array("new/markers/smallgroups", "edit/markers/smallgroups"))
	def saveAndExit(@ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand, @PathVariable assignment: Assignment): Mav =  {
		assignMarkersCmd.apply()
		Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddSubmissions), value = Array("new/markers/smallgroups"))
	def submitAndAddSubmissionsCreate(@ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand): Mav = {
		val assignment = assignMarkersCmd.apply()
		RedirectForce(Routes.admin.assignment.createOrEditSubmissions(assignment, createMode))
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddSubmissions), value = Array("edit/markers/smallgroups"))
	def submitAndAddSubmissionsEdit(@ModelAttribute("assignMarkersCommand") assignMarkersCmd: AssignMarkersCommand): Mav = {
		val assignment = assignMarkersCmd.apply()
		RedirectForce(Routes.admin.assignment.createOrEditSubmissions(assignment, editMode))
	}

}
