package uk.ac.warwick.tabula.groups.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.groups.commands.admin.{SetAllocation, SmallGroupsMarkerAllocationCommand}
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController

@Controller
@RequestMapping(value=Array("/admin/marker-allocation/{assignment}"))
class SmallGroupsMarkerAllocationController extends GroupsController {

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment) = {
		SmallGroupsMarkerAllocationCommand(assignment)
	}

	@RequestMapping(method=Array(GET))
	def getGroupMembership(
		@PathVariable assignment:Assignment,
		@ModelAttribute("command") cmd: Appliable[Seq[SetAllocation]]
	) = {

		val allocations = cmd.apply()

		Mav("admin/module/marker-allocation",
			"sets" -> allocations.map(_.set),
			"allocations" -> allocations,
			"hasSecondMarker" -> assignment.markingWorkflow.hasSecondMarker,
			"firstMarkerRole" -> assignment.markingWorkflow.firstMarkerRoleName,
			"secondMarkerRole" -> assignment.markingWorkflow.secondMarkerRoleName.getOrElse("Second marker")
		).noLayout()
	}
}
