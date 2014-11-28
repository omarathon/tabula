package uk.ac.warwick.tabula.coursework.web.controllers.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.helpers.MarkerFeedbackCollecting
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{MarkingMethod, Assignment, Module}
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.data.model.MarkingMethod.ModeratedMarking
import uk.ac.warwick.tabula.data.model.MarkingState.{Rejected, MarkingCompleted}
import uk.ac.warwick.tabula.coursework.commands.feedback.{OnlineMarkerFeedbackFormCommand, OnlineMarkerFeedbackCommand}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/marker/{marker}/feedback/online"))
class OnlineMarkerFeedbackController extends CourseworkController with MarkerFeedbackCollecting with AutowiringUserLookupComponent {

	@ModelAttribute
	def command(@PathVariable module: Module,
							@PathVariable assignment: Assignment,
							@PathVariable marker: User,
							submitter: CurrentUser) =
		OnlineMarkerFeedbackCommand(module, assignment, marker, submitter)

	@RequestMapping
	def showTable(@ModelAttribute command: OnlineMarkerFeedbackCommand, errors: Errors): Mav = {

		val feedbackGraphs = command.apply()
		val (assignment, module) = (command.assignment, command.assignment.module)

		val markerFeedbackCollections = getMarkerFeedbackCollections(assignment, module, command.marker, userLookup)

		// will need to take into account Seen Second Marking also
		val showMarkingCompleted =
			assignment.markingWorkflow.markingMethod != ModeratedMarking || assignment.isFirstMarker(command.marker)

		Mav("admin/assignments/feedback/online_framework",
			"showMarkingCompleted" -> showMarkingCompleted,
			"showGenericFeedback" -> false,
			"assignment" -> assignment,
			"command" -> command,
			"studentFeedbackGraphs" -> feedbackGraphs,
			"inProgressFeedback" -> markerFeedbackCollections.inProgressFeedback,
			"completedFeedback" -> markerFeedbackCollections.completedFeedback,
			"rejectedFeedback" -> markerFeedbackCollections.rejectedFeedback,
			"onlineMarkingUrls" -> feedbackGraphs.map{ graph =>
				graph.student.getUserId -> assignment.markingWorkflow.onlineMarkingUrl(assignment, command.marker, graph.student.getUserId)
			}.toMap
		).crumbs(
				Breadcrumbs.Standard(s"Marking for ${assignment.name}", Some(Routes.admin.assignment.markerFeedback(assignment, command.marker)), "")
			)
	}
}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/feedback/online"))
class OnlineMarkerFeedbackControllerCurrentUser extends CourseworkController {

	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, currentUser: CurrentUser) = {
		Redirect(Routes.admin.assignment.markerFeedback.onlineFeedback(assignment, currentUser.apparentUser))
	}
}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/marker/{marker}/feedback/online/{student}"))
class OnlineMarkerFeedbackFormController extends CourseworkController {

	validatesSelf[OnlineMarkerFeedbackFormCommand]

	@ModelAttribute("command")
	def command(@PathVariable student: User,
							@PathVariable module: Module,
							@PathVariable assignment: Assignment,
							@PathVariable marker: User,
							submitter: CurrentUser) =
		OnlineMarkerFeedbackFormCommand(module, assignment, student, marker, submitter)

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
			"isModerated" -> (command.assignment.markingWorkflow.markingMethod == MarkingMethod.ModeratedMarking)
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
