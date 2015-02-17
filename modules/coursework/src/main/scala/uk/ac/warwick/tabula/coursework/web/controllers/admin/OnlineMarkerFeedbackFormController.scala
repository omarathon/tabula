package uk.ac.warwick.tabula.coursework.web.controllers.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{MarkingMethod, Assignment, Module}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.MarkingState.{Rejected, MarkingCompleted}
import uk.ac.warwick.tabula.coursework.commands.feedback.{GenerateGradesFromMarkCommand, OnlineMarkerFeedbackFormCommand}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/marker/{marker}/feedback/online/{student}"))
class OnlineMarkerFeedbackFormController extends CourseworkController {

	validatesSelf[OnlineMarkerFeedbackFormCommand]

	@ModelAttribute("command")
	def command(
		@PathVariable student: User,
		@PathVariable module: Module,
		@PathVariable assignment: Assignment,
		@PathVariable marker: User,
		submitter: CurrentUser
	) =	OnlineMarkerFeedbackFormCommand(
		mandatory(module),
		mandatory(assignment),
		student,
		marker,
		submitter,
		GenerateGradesFromMarkCommand(mandatory(module), mandatory(assignment))
	)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute("command") command: OnlineMarkerFeedbackFormCommand, errors: Errors): Mav = {

		val isCompleted = command.allMarkerFeedbacks.forall(_.state == MarkingCompleted)

		val parentFeedback = command.allMarkerFeedbacks.head.feedback
		val isRejected = command.allMarkerFeedbacks.exists(_.state == Rejected)
		val isCurrentUserFeebackEntry = parentFeedback.getCurrentWorkflowFeedback.exists(_.getMarkerUser == command.marker)
		val allCompletedMarkerFeedback = parentFeedback.getAllCompletedMarkerFeedback


		Mav("admin/assignments/feedback/marker_online_feedback" ,
			"command" -> command,
			"isCompleted" -> isCompleted,
			"isRejected" -> isRejected,
			"allCompletedMarkerFeedback" -> allCompletedMarkerFeedback,
			"isCurrentUserFeedbackEntry" -> isCurrentUserFeebackEntry,
			"parentFeedback" -> parentFeedback,
			"secondMarkerNotes" -> Option(parentFeedback.secondMarkerFeedback).map(_.rejectionComments).orNull,
			"isModerated" -> (command.assignment.markingWorkflow.markingMethod == MarkingMethod.ModeratedMarking),
			"isFinalMarking" -> Option(parentFeedback.secondMarkerFeedback).exists(_.state == MarkingCompleted),
			"isGradeValidation" -> command.module.adminDepartment.assignmentGradeValidation
		).noLayout()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("command") @Valid command: OnlineMarkerFeedbackFormCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			showForm(command, errors)
		} else {
			command.apply()
			Mav("ajax_success").noLayout()
		}
	}

}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/feedback/online/{student}"))
class OnlineMarkerFeedbackFormControllerCurrentUser extends CourseworkController {

	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, @PathVariable student: User, currentUser: CurrentUser) = {
		Redirect(Routes.admin.assignment.markerFeedback.onlineFeedback.student(assignment, currentUser.apparentUser, student))
	}
}
