package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.coursework.commands.UploadFeedbackToSitsCommand
import uk.ac.warwick.tabula.coursework.commands.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{Feedback, Assignment, Module}

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/upload-to-sits"))
class UploadFeedbackToSitsController extends CourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable assignment: Assignment) =
		UploadFeedbackToSitsCommand(
			mandatory(module),
			mandatory(assignment),
			user,
			GenerateGradesFromMarkCommand(mandatory(module), mandatory(assignment))
		)

	@RequestMapping(method = Array(GET))
	def form(@PathVariable module: Module) = {
		Mav("admin/assignments/publish/upload_to_sits",
			"isGradeValidation" -> module.adminDepartment.assignmentGradeValidation
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("command") cmd: Appliable[Seq[Feedback]], @PathVariable assignment: Assignment) = {
		cmd.apply()
		Redirect(Routes.admin.assignment.submissionsandfeedback(assignment))
	}

}
