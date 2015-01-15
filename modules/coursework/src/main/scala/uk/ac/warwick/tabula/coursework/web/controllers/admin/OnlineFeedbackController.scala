package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.tabula.coursework.commands.feedback.{OnlineFeedbackFormCommand, OnlineFeedbackCommand}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.CurrentUser
import javax.validation.Valid
import uk.ac.warwick.tabula.coursework.web.Routes

import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback/online"))
class OnlineFeedbackController extends CourseworkController with AutowiringUserLookupComponent {

	@ModelAttribute
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment, submitter: CurrentUser) =
		OnlineFeedbackCommand(module, assignment, submitter)

	@RequestMapping
	def showTable(@ModelAttribute command: OnlineFeedbackCommand, errors: Errors): Mav = {

		val feedbackGraphs = command.apply()
		val (assignment, module) = (command.assignment, command.assignment.module)

		Mav("admin/assignments/feedback/online_framework",
			"showMarkingCompleted" -> false,
			"showGenericFeedback" -> true,
			"assignment" -> assignment,
			"command" -> command,
			"studentFeedbackGraphs" -> feedbackGraphs,
			"onlineMarkingUrls" -> feedbackGraphs.map{ graph =>
				graph.student.getUserId -> Routes.admin.assignment.onlineFeedback(assignment)
			}.toMap
		).crumbs(
				Breadcrumbs.Department(module.adminDepartment),
				Breadcrumbs.Module(module)
			)
	}

}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback/online/{student}"))
class OnlineFeedbackFormController extends CourseworkController {

	validatesSelf[OnlineFeedbackFormCommand]

	@ModelAttribute("command")
	def command(@PathVariable student: User, @PathVariable module: Module, @PathVariable assignment: Assignment, currentUser: CurrentUser) =
		OnlineFeedbackFormCommand(module, assignment, student, currentUser.apparentUser, currentUser)

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

