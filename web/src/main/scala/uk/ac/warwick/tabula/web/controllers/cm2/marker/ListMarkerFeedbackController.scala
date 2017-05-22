package uk.ac.warwick.tabula.web.controllers.cm2.marker

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments.markers.ListMarkerFeedbackCommand.EnhancedFeedbackByStage
import uk.ac.warwick.tabula.commands.cm2.assignments.markers.{ListMarkerFeedbackCommand, ListMarkerFeedbackState}
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.userlookup.User


@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/marker/{marker}/list"))
class ListMarkerFeedbackController extends CourseworkController {

	type Command = Appliable[EnhancedFeedbackByStage] with ListMarkerFeedbackState

	@ModelAttribute
	def command(@PathVariable assignment: Assignment, @PathVariable marker: User, currentUser: CurrentUser): Command =
		ListMarkerFeedbackCommand(assignment, marker, currentUser)


	@RequestMapping(method = Array(GET, HEAD))
	def list(@ModelAttribute command: Command): Mav = {
		Mav("cm2/admin/assignments/markers/marker_list",
			"department" -> command.assignment.module.adminDepartment,
			"assignment" -> command.assignment,
			"workflowType" -> command.assignment.cm2MarkingWorkflow.workflowType,
			"marker" -> command.marker,
			"feedbackByStage" -> command.apply()
		)
	}

}
