package uk.ac.warwick.tabula.web.controllers.cm2.marker

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.cm2.assignments.markers.{MarkerAdjustmentDirection, MarkerAdjustmentType, MarkerBulkAdjustmentCommand, MarkerBulkAdjustmentState}
import uk.ac.warwick.tabula.commands.cm2.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.userlookup.User

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/marker/{marker}/{stage}/bulk-adjustment"))
class MarkerBulkAdjustmentController  extends CourseworkController {
	validatesSelf[SelfValidating]

	type Command = Appliable[Seq[MarkerFeedback]] with MarkerBulkAdjustmentState

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment, @PathVariable marker: User, @PathVariable stage: MarkingWorkflowStage, currentUser: CurrentUser): Command =
		MarkerBulkAdjustmentCommand(mandatory(assignment), mandatory(marker), currentUser, mandatory(stage), GenerateGradesFromMarkCommand(assignment))

	@RequestMapping(method = Array(GET))
	def showForm(
		@PathVariable assignment: Assignment,
		@PathVariable marker: User,
		@PathVariable stage: MarkingWorkflowStage,
		@ModelAttribute("command") command: Command,
		errors: Errors
	): Mav = {
		Mav("cm2/admin/assignments/markers/bulk_adjustments",
			"previousMarkers" -> command.previousMarkers,
			"directions" -> MarkerAdjustmentDirection.values,
			"adjustmentTypes" -> MarkerAdjustmentType.values
		).noLayout()
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@PathVariable assignment: Assignment,
		@PathVariable marker: User,
		@PathVariable stage: MarkingWorkflowStage,
		@Valid @ModelAttribute("command") command: Command,
		errors: Errors
	): Mav = {
		if (errors.hasErrors)
			showForm(assignment, marker, stage, command, errors)
		else {
			command.apply()
			Mav(new JSONView(Map(
				"status" -> "successful",
				"redirect" -> Routes.admin.assignment.markerFeedback(assignment, marker)
			)))
		}
	}
}
