package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments


import javax.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments._
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav

import scala.collection.JavaConverters._

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}"))
class ModifyAssignmentMarkersSmallGroupsController extends AbstractAssignmentController {

	type SmallGroupCommand = Appliable[Assignment] with AssignMarkersSmallGroupsState with AssignMarkersSmallGroupsCommandRequest with PopulateOnForm

	validatesSelf[SelfValidating]

	@ModelAttribute("smallGroupCommand")
	def smallGroupCommand(@PathVariable assignment: Assignment) = AssignMarkersSmallGroupsCommand(mustBeCM2(mandatory(assignment)))

	private def form(assignment: Assignment, smallGroupCommand: SmallGroupCommand, mode:String): Mav = {
		smallGroupCommand.populate()

		val module =  mandatory(assignment.module)
		val allocations = smallGroupCommand.markerAllocations
		val workflow = assignment.cm2MarkingWorkflow

		Mav("cm2/admin/assignments/assignment_markers_smallgroups",
			"module" -> module,
			"sets" -> smallGroupCommand.setAllocations.map(_.set),
			"setsMap" -> smallGroupCommand.setAllocations.map(a => a.set.id -> a).toMap.asJava,
			"allocations" -> allocations,
			"allocationOrder" -> workflow.allocationOrder,
			"stageNames" -> workflow.allStages.groupBy(_.allocationName).mapValues(_.map(_.name)),
			"mode" -> mode,
			"allocationWarnings" -> smallGroupCommand.allocationWarnings)
			.crumbsList(Breadcrumbs.assignment(assignment))
	}

	@RequestMapping(method = Array(GET, HEAD), value = Array("new/markers/smallgroups"))
	def createForm(
		@PathVariable assignment: Assignment,
		@ModelAttribute("smallGroupCommand") smallGroupCommand: SmallGroupCommand
	): Mav = {
		form(assignment, smallGroupCommand, createMode)
	}

	@RequestMapping(method = Array(GET, HEAD), value = Array("edit/markers/smallgroups"))
	def editForm(
		@PathVariable assignment: Assignment,
		@ModelAttribute("smallGroupCommand") smallGroupCommand: SmallGroupCommand
	): Mav = form(assignment, smallGroupCommand, editMode)

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddMarkers), value = Array("new/markers/smallgroups", "edit/markers/smallgroups"))
	def saveAndExit(@PathVariable assignment: Assignment, @Valid @ModelAttribute("smallGroupCommand") smallGroupCommand: SmallGroupCommand, errors: BindingResult): Mav =  {
		if (errors.hasErrors)
			form(assignment, smallGroupCommand, editMode)
		else {
			smallGroupCommand.apply()
			Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddSubmissions), value = Array("new/markers/smallgroups"))
	def submitAndAddSubmissionsCreate(@PathVariable assignment: Assignment, @Valid @ModelAttribute("smallGroupCommand") smallGroupCommand: SmallGroupCommand, errors: BindingResult): Mav = {
		if (errors.hasErrors)
			form(assignment, smallGroupCommand, createMode)
		else {
			smallGroupCommand.apply()
			RedirectForce(Routes.admin.assignment.createOrEditSubmissions(assignment, createMode))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(ManageAssignmentMappingParameters.createAndAddSubmissions), value = Array("edit/markers/smallgroups"))
	def submitAndAddSubmissionsEdit(@PathVariable assignment: Assignment, @Valid @ModelAttribute("smallGroupCommand") smallGroupCommand: SmallGroupCommand, errors: BindingResult): Mav = {
		if (errors.hasErrors)
			form(assignment, smallGroupCommand, editMode)
		else {
			smallGroupCommand.apply()
			RedirectForce(Routes.admin.assignment.createOrEditSubmissions(assignment, editMode))
		}
	}

}
