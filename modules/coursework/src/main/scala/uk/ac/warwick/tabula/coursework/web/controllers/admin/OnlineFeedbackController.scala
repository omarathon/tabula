package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.tabula.coursework.helpers.MarkerFeedbackCollecting
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.coursework.commands.feedback.{OnlineMarkerFeedbackFormCommand, OnlineMarkerFeedbackCommand, OnlineFeedbackFormCommand, OnlineFeedbackCommand}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupService}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.CurrentUser
import javax.validation.Valid
import uk.ac.warwick.tabula.data.model.MarkingState.{Rejected, MarkingCompleted}
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.data.model.MarkingMethod.ModeratedMarking
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback/online"))
class OnlineFeedbackController extends CourseworkController with MarkerFeedbackCollecting with AutowiringUserLookupComponent {

	@ModelAttribute
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		OnlineFeedbackCommand(module, assignment)

	@RequestMapping
	def showTable(@ModelAttribute command: OnlineFeedbackCommand, errors: Errors): Mav = {

		val feedbackGraphs = command.apply()
		val (assignment, module) = (command.assignment, command.assignment.module)

		val markerFeedbackCollections = getMarkerFeedbackCollections(assignment, module, user, userLookup)

		Mav("admin/assignments/feedback/online_framework",
			"showMarkingCompleted" -> false,
			"showGenericFeedback" -> true,
			"assignment" -> assignment,
			"command" -> command,
			"inProgressFeedback" -> markerFeedbackCollections.inProgressFeedback,
			"completedFeedback" -> markerFeedbackCollections.completedFeedback,
			"rejectedFeedback" -> markerFeedbackCollections.rejectedFeedback,
			"studentFeedbackGraphs" -> feedbackGraphs,
			"onlineMarkingUrls" -> feedbackGraphs.map{ graph =>
				graph.student.getUserId -> Routes.admin.assignment.onlineFeedback(assignment)
			}.toMap
		).crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module), Breadcrumbs.Current(s"Online marking for ${assignment.name}"))
	}



}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/marker/feedback/online"))
class OnlineMarkerFeedbackController extends CourseworkController with MarkerFeedbackCollecting with AutowiringUserLookupComponent {

	@ModelAttribute
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment, currentUser: CurrentUser) =
		OnlineMarkerFeedbackCommand(module, assignment, currentUser.apparentUser)

	@RequestMapping
	def showTable(@ModelAttribute command: OnlineMarkerFeedbackCommand, errors: Errors): Mav = {

		val feedbackGraphs = command.apply()
		val (assignment, module) = (command.assignment, command.assignment.module)

		val markerFeedbackCollections = getMarkerFeedbackCollections(assignment, module, user, userLookup)

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
		).crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module), Breadcrumbs.Current(s"Online marking for ${assignment.name}"))
	}
}


@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback/online/{student}"))
class OnlineFeedbackFormController extends CourseworkController {

	validatesSelf[OnlineFeedbackFormCommand]

	@ModelAttribute("command")
	def command(@PathVariable student: User, @PathVariable module: Module, @PathVariable assignment: Assignment, currentUser: CurrentUser) =
		OnlineFeedbackFormCommand(module, assignment, student, currentUser)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute("command") command: OnlineFeedbackFormCommand, errors: Errors): Mav = {

		Mav("admin/assignments/feedback/online_feedback",
			"command" -> command).noLayout()
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("command") @Valid command: OnlineFeedbackFormCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			showForm(command, errors)
		} else {
			command.apply()
			Mav("ajax_success").noLayout()
		}
	}

}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/marker/feedback/online/{student}"))
class OnlineMarkerFeedbackFormController extends CourseworkController {

	validatesSelf[OnlineMarkerFeedbackFormCommand]

	@ModelAttribute("command")
	def command(@PathVariable student: User, @PathVariable module: Module, @PathVariable assignment: Assignment, currentUser: CurrentUser) =
		OnlineMarkerFeedbackFormCommand(module, assignment, student, currentUser)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute("command") command: OnlineMarkerFeedbackFormCommand, errors: Errors): Mav = {

		val isCompleted = command.allMarkerFeedbacks.forall(_.state == MarkingCompleted)

		val parentFeedback = command.allMarkerFeedbacks.head.feedback
		val isRejected = command.allMarkerFeedbacks.exists(_.state == Rejected)
		val isCurrentUserFeebackEntry = parentFeedback.getCurrentWorkflowFeedback.exists(_.getMarkerUser == command.currentUser.apparentUser)
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