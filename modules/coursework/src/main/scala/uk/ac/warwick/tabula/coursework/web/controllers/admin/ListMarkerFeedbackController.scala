package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.coursework.helpers.MarkerFeedbackCollections
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import uk.ac.warwick.tabula.coursework.commands.assignments.ListMarkerFeedbackCommand
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.coursework.helpers.MarkerFeedbackCollections

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/list"))
class ListMarkerFeedbackController  extends CourseworkController {

	@ModelAttribute("command")
	def createCommand(@PathVariable assignment: Assignment, @PathVariable module: Module, user: CurrentUser) =
		ListMarkerFeedbackCommand(assignment, module, user)

	@RequestMapping(method = Array(HEAD, GET))
	def list(@ModelAttribute("command") command: Appliable[MarkerFeedbackCollections], @PathVariable assignment: Assignment): Mav = {
		val markerFeedbackCollections = command.apply()
		val inProgressFeedback = markerFeedbackCollections.inProgressFeedback
		val completedFeedback = markerFeedbackCollections.completedFeedback
		val rejectedFeedback = markerFeedbackCollections.rejectedFeedback

		val maxFeedbackCount = Math.max(
			rejectedFeedback.map(_.feedbacks.size).reduceOption(_ max _).getOrElse(0),
			Math.max(
				inProgressFeedback.map(_.feedbacks.size).reduceOption(_ max _).getOrElse(0),
				completedFeedback.map(_.feedbacks.size).reduceOption(_ max _).getOrElse(0)
			)
		)
		val hasFirstMarkerFeedback = maxFeedbackCount > 1
		val hasSecondMarkerFeedback = maxFeedbackCount > 2

		Mav("admin/assignments/markerfeedback/list",
			"assignment" -> assignment,
			"inProgressFeedback" -> inProgressFeedback,
			"completedFeedback" -> completedFeedback,
			"rejectedFeedback" -> rejectedFeedback,
			"firstMarkerRoleName" -> assignment.markingWorkflow.firstMarkerRoleName,
			"secondMarkerRoleName" -> assignment.markingWorkflow.secondMarkerRoleName,
			"thirdMarkerRoleName" -> assignment.markingWorkflow.thirdMarkerRoleName,
			"hasFirstMarkerFeedback" -> hasFirstMarkerFeedback,
			"hasSecondMarkerFeedback" -> hasSecondMarkerFeedback
		)
	}

}
