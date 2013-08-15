package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Member, Assignment, Module}
import uk.ac.warwick.tabula.coursework.commands.feedback.{OnlineFeedbackFormCommand, OnlineFeedbackCommand}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav

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
			"assignment" -> assignment,
			"command" -> command,
			"studentFeedbackGraphs" -> feedbackGraphs)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module), Breadcrumbs.Current(s"Online marking for ${assignment.name}"))
	}
}


@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/feedback/online/{student}"))
class OnlineFeedbackFormController extends CourseworkController {

	@ModelAttribute
	def command(@PathVariable student: Member, @PathVariable module: Module, @PathVariable assignment: Assignment) =
		OnlineFeedbackFormCommand(module, assignment, student)

	@RequestMapping
	def showForm(@ModelAttribute command: OnlineFeedbackFormCommand, errors: Errors): Mav = {

		Mav("admin/assignments/feedback/online_feedback",
			"command" -> command).noLayout
	}
}