package uk.ac.warwick.tabula.web.controllers.cm2.marker

import javax.validation.Valid

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.cm2.assignments.markers.{OnlineMarkerFeedbackCommand, OnlineMarkerFeedbackState}
import uk.ac.warwick.tabula.commands.cm2.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowStage
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.PreventCaching
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.userlookup.User

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/marker/{marker}/{stage}/feedback/online/{student}"))
class OnlineMarkerFeedbackController extends CourseworkController with PreventCaching {

	validatesSelf[SelfValidating]

	type Command = Appliable[MarkerFeedback] with OnlineMarkerFeedbackState

	@ModelAttribute("command")
	def command(
		@PathVariable assignment: Assignment,
		@PathVariable stage: MarkingWorkflowStage,
		@PathVariable student: User,
		@PathVariable marker: User,
		submitter: CurrentUser
	) = OnlineMarkerFeedbackCommand(
		mandatory(assignment),
		mandatory(stage),
		mandatory(student),
		mandatory(marker),
		submitter,
		GenerateGradesFromMarkCommand(mandatory(assignment))
	)

	@RequestMapping
	def showForm(@ModelAttribute("command") command: Command, errors: Errors): Mav = {
		Mav("cm2/admin/assignments/markers/marker_online_feedback",
			"isGradeValidation" -> command.assignment.module.adminDepartment.assignmentGradeValidation,
			"command" -> command
		).noLayout()
	}

	@RequestMapping(method = Array(POST))
	def submit(@PathVariable stage: MarkingWorkflowStage, @Valid @ModelAttribute("command") command: Command, errors: Errors): Mav = {
		if (errors.hasErrors) {
			showForm(command, errors)
		} else {
			command.apply()
			Mav("ajax_success").noLayout()
		}
	}

}
