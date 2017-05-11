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
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.userlookup.User

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/marker/{marker}/feedback/online/{student}"))
class OnlineMarkerFeedbackController extends CourseworkController {

	validatesSelf[SelfValidating]

	type Command = Appliable[MarkerFeedback] with OnlineMarkerFeedbackState

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment, @PathVariable student: User, @PathVariable marker: User, submitter: CurrentUser) = OnlineMarkerFeedbackCommand(
		mandatory(assignment),
		mandatory(student),
		mandatory(marker),
		submitter,
		GenerateGradesFromMarkCommand(mandatory(assignment.module), mandatory(assignment))
	)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute("command") command: Command, errors: Errors): Mav = {
		Mav(s"$urlPrefix/admin/assignments/markers/marker_online_feedback",
			"isGradeValidation" -> command.assignment.module.adminDepartment.assignmentGradeValidation,
			"command" -> command
		).noLayout()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("command") @Valid command: Command, errors: Errors): Mav = {
		if (errors.hasErrors) {
			showForm(command, errors)
		} else {
			command.apply()
			Mav("ajax_success").noLayout()
		}
	}

	@RequestMapping(method = Array(GET, HEAD), value = Array("test"))
	def test(@PathVariable assignment: Assignment, @PathVariable student: User, @PathVariable marker: User): Mav = {
		Mav(s"$urlPrefix/admin/assignments/markers/online_feedback_test", "assignment" -> assignment, "student" -> student, "marker" -> marker)
	}

}
