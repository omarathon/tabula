package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{Module, Assignment}
import uk.ac.warwick.tabula.coursework.commands.assignments.ListMarkerFeedbackCommand
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.coursework.helpers.MarkerFeedbackCollections
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/{marker}/list"))
class ListMarkerFeedbackController extends CourseworkController {

	@ModelAttribute("command")
	def createCommand(@PathVariable assignment: Assignment,
										@PathVariable module: Module,
										@PathVariable marker: User,
										submitter: CurrentUser) =
		ListMarkerFeedbackCommand(assignment, module, marker, submitter)

	@RequestMapping(method = Array(HEAD, GET))
	def list(@ModelAttribute("command") command: Appliable[MarkerFeedbackCollections], @PathVariable assignment: Assignment, @PathVariable marker: User): Mav = {

		if(assignment.markingWorkflow == null) {
			Mav("errors/no_workflow", "assignmentUrl" -> Routes.admin.assignment.submissionsandfeedback.summary(assignment))
		} else {
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

			val hasOriginalityReport =
				Seq(inProgressFeedback, completedFeedback, rejectedFeedback)
					.exists { _.exists { _.submission.hasOriginalityReport }}

			Mav("admin/assignments/markerfeedback/list",
				"assignment" -> assignment,
				"inProgressFeedback" -> inProgressFeedback,
				"completedFeedback" -> completedFeedback,
				"rejectedFeedback" -> rejectedFeedback,
				"firstMarkerRoleName" -> assignment.markingWorkflow.firstMarkerRoleName,
				"secondMarkerRoleName" -> assignment.markingWorkflow.secondMarkerRoleName,
				"thirdMarkerRoleName" -> assignment.markingWorkflow.thirdMarkerRoleName,
				"hasFirstMarkerFeedback" -> hasFirstMarkerFeedback,
				"hasSecondMarkerFeedback" -> hasSecondMarkerFeedback,
				"hasOriginalityReport" -> hasOriginalityReport,
				"marker" -> marker
			)
		}
	}

}

// Redirects users trying to access a marking workflow using the old style URL
@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/marker/list"))
class ListCurrentUsersMarkerFeedbackController extends CourseworkController {
	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, currentUser: CurrentUser) = {
		Redirect(Routes.admin.assignment.markerFeedback(assignment, currentUser.apparentUser))
	}
}
