package uk.ac.warwick.tabula.web.controllers.cm2.marker

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments.markers.{AddMarksCommandBindListener, MarkerAddMarksCommand, MarkerAddMarksState}
import uk.ac.warwick.tabula.commands.cm2.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.data.model.{Assignment, MarkerFeedback}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.userlookup.User

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/marker/{marker}/marks"))
class MarkerUploadMarksController extends CourseworkController {

	type Command = Appliable[Seq[MarkerFeedback]] with MarkerAddMarksState with AddMarksCommandBindListener

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment, @PathVariable marker: User, currentUser: CurrentUser): Command =
		MarkerAddMarksCommand(mandatory(assignment), mandatory(marker), currentUser, GenerateGradesFromMarkCommand(assignment))

	@RequestMapping
	def viewForm(@ModelAttribute("command") cmd: Command, @PathVariable assignment: Assignment, @PathVariable marker: User, errors: Errors): Mav = {
		Mav("cm2/admin/assignments/upload_marks",
			"isGradeValidation" -> cmd.assignment.module.adminDepartment.assignmentGradeValidation,
			"templateUrl" -> Routes.admin.assignment.markerFeedback.marksTemplate(assignment, marker),
			"formUrl" -> Routes.admin.assignment.markerFeedback.marks(assignment, marker),
			"cancelUrl" -> Routes.admin.assignment.markerFeedback(assignment, marker)
		).crumbsList(Breadcrumbs.markerAssignment(assignment, marker, proxying = cmd.isProxying))
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def preview(@ModelAttribute("command") cmd: Command, @PathVariable assignment: Assignment, @PathVariable marker: User, errors: Errors): Mav = {
		if (errors.hasErrors) viewForm(cmd, assignment, marker, errors)
		else {
			cmd.postBindValidation(errors)
			Mav("cm2/admin/assignments/upload_marks_preview",
				"formUrl" -> Routes.admin.assignment.markerFeedback.marks(assignment, marker),
				"cancelUrl" -> Routes.admin.assignment.markerFeedback(assignment, marker)
			).crumbsList(Breadcrumbs.markerAssignment(assignment, marker, proxying = cmd.isProxying))
		}
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def upload(@ModelAttribute("command") cmd: Command, @PathVariable assignment: Assignment, @PathVariable marker: User, errors: Errors): Mav = {
		cmd.apply()
		Redirect(Routes.admin.assignment.markerFeedback(assignment, marker))
	}

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/marker/marks"))
class CurrentMarkerUploadMarksController extends CourseworkController {

	@RequestMapping
	def redirect(@PathVariable assignment: Assignment, currentUser: CurrentUser): Mav = {
		Redirect(Routes.admin.assignment.markerFeedback.marks(assignment, currentUser.apparentUser))
	}
}
