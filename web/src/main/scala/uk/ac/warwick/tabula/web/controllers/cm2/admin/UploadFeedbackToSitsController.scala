package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.UploadFeedbackToSitsCommand
import uk.ac.warwick.tabula.commands.cm2.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Profile(Array("cm2Enabled")) @Controller
@RequestMapping(value=Array("/${cm2.prefix}/admin/assignments/{assignment}/upload-to-sits"))
class UploadFeedbackToSitsController extends CourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment) =
		UploadFeedbackToSitsCommand(
			mandatory(assignment),
			user,
			GenerateGradesFromMarkCommand(mandatory(assignment))
		)

	@RequestMapping(params = Array("!confirm"))
	def form(@ModelAttribute("command") cmd: Appliable[Seq[Feedback]], @PathVariable assignment: Assignment): Mav =
		Mav("cm2/admin/assignments/publish/upload_to_sits",
			"isGradeValidation" -> assignment.module.adminDepartment.assignmentGradeValidation)
			.crumbsList(Breadcrumbs.assignment(assignment))

	@RequestMapping(method = Array(POST), params = Array("confirm"))
	def submit(@ModelAttribute("command") cmd: Appliable[Seq[Feedback]], @PathVariable assignment: Assignment): Mav = {
		cmd.apply()
		Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))
	}

}
