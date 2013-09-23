package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Member, Assignment, Module}
import uk.ac.warwick.tabula.coursework.commands.feedback.{OnlineMarkerFeedbackFormCommand, OnlineMarkerFeedbackCommand, OnlineFeedbackFormCommand, OnlineFeedbackCommand}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.CurrentUser
import javax.validation.Valid
import uk.ac.warwick.tabula.data.model.MarkingState.MarkingCompleted

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback/online"))
class OnlineFeedbackController extends CourseworkController {

	@ModelAttribute
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		OnlineFeedbackCommand(module, assignment)

	@RequestMapping
	def showTable(@ModelAttribute command: OnlineFeedbackCommand, errors: Errors): Mav = {

		val feedbackGraphs = command.apply()
		val (assignment, module) = (command.assignment, command.assignment.module)

		Mav("admin/assignments/feedback/online_framework",
			"canAddGenericFeedback" -> true,
			"assignment" -> assignment,
			"command" -> command,
			"studentFeedbackGraphs" -> feedbackGraphs)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module), Breadcrumbs.Current(s"Online marking for ${assignment.name}"))
	}
}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/marker/feedback/online"))
class OnlineMarkerFeedbackController extends CourseworkController {

	@ModelAttribute
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment, currentUser: CurrentUser) =
		OnlineMarkerFeedbackCommand(module, assignment, currentUser.apparentUser)

	@RequestMapping
	def showTable(@ModelAttribute command: OnlineMarkerFeedbackCommand, errors: Errors): Mav = {

		val feedbackGraphs = command.apply()
		val (assignment, module) = (command.assignment, command.assignment.module)

		Mav("admin/assignments/feedback/online_framework",
			"canAddGenericFeedback" -> false,
			"assignment" -> assignment,
			"command" -> command,
			"studentFeedbackGraphs" -> feedbackGraphs)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module), Breadcrumbs.Current(s"Online marking for ${assignment.name}"))
	}
}


@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback/online/{student}"))
class OnlineFeedbackFormController extends CourseworkController {

	validatesSelf[OnlineFeedbackFormCommand]

	@ModelAttribute("command")
	def command(@PathVariable student: Member, @PathVariable module: Module, @PathVariable assignment: Assignment, currentUser: CurrentUser) =
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
	def command(@PathVariable student: Member, @PathVariable module: Module, @PathVariable assignment: Assignment, currentUser: CurrentUser) =
		OnlineMarkerFeedbackFormCommand(module, assignment, student, currentUser)

	@RequestMapping(method = Array(GET, HEAD))
	def showForm(@ModelAttribute("command") command: OnlineMarkerFeedbackFormCommand, errors: Errors): Mav = {

		val (isCompleted, firstMarkerFeedback) = command.markerFeedback match {
			case Some(mf) => (mf.state == MarkingCompleted, mf.feedback.firstMarkerFeedback)
			case None => (false, None)
		}

		Mav("admin/assignments/feedback/marker_online_feedback",
			"command" -> command,
			"isCompleted" -> isCompleted,
			"isFirstMarker" -> command.assignment.isFirstMarker(command.currentUser.apparentUser),
			"firstMarkerFeedback" -> firstMarkerFeedback
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