package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.AddMarkerFeedbackCommand
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.userlookup.User

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/marker/{marker}/feedback"))
class AddMarkerFeedbackController extends CourseworkController {

	@ModelAttribute
	def command(
		@PathVariable assignment: Assignment,
		@PathVariable marker: User,
		user: CurrentUser
	) = {
		mandatory(Option(assignment.cm2MarkingWorkflow))
		new AddMarkerFeedbackCommand(assignment, marker, user)
	}

	@RequestMapping
	def uploadForm(
		@PathVariable assignment: Assignment,
		@PathVariable marker: User,
		@ModelAttribute cmd: AddMarkerFeedbackCommand
	): Mav = {
		Mav("cm2/admin/assignments/markerfeedback/form",
			"isProxying" -> cmd.isProxying,
			"proxyingAs" -> marker
		)
		//	crumbs(
		//	CourseworkBreadcrumbs.Standard(s"Marking for ${assignment.name}", Some(Routes.admin.assignment.markerFeedback(assignment, marker)), "")
		//)
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmUpload(
		@PathVariable assignment: Assignment,
		@PathVariable marker: User,
		@ModelAttribute cmd: AddMarkerFeedbackCommand,
		errors: Errors): Mav = {
		cmd.preExtractValidation(errors)
		if (errors.hasErrors) {
			uploadForm(assignment, marker, cmd)
		} else {
			cmd.postExtractValidation(errors)
			cmd.processStudents()
			val test = cmd.markedStudents.size()
			Mav("cm2/admin/assignments/markerfeedback/preview",
				"isProxying" -> cmd.isProxying,
				"proxyingAs" -> marker
			)
			//.crumbs(
			//CourseworkBreadcrumbs.Standard(s"Marking for ${assignment.name}", Some(Routes.admin.assignment.markerFeedback(assignment, marker)), "")
			//)
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(
		@PathVariable assignment: Assignment,
		@PathVariable marker: User,
		@ModelAttribute cmd: AddMarkerFeedbackCommand, errors: Errors): Mav = {

		cmd.preExtractValidation(errors)
		cmd.postExtractValidation(errors)

		if (errors.hasErrors) {
			confirmUpload(assignment, marker, cmd, errors)
		} else {
			// do apply, redirect back
			cmd.apply()
			Redirect(Routes.admin.assignment.markerFeedback(assignment, marker))
		}
	}

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/marker/feedback"))
class AddMarkerFeedbackControllerCurrentUser extends CourseworkController {

	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, currentUser: CurrentUser): Mav = {
		Redirect(Routes.admin.assignment.markerFeedback.feedback(assignment, currentUser.apparentUser))
	}
}