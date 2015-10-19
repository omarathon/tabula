package uk.ac.warwick.tabula.exams.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.UploadFeedbackToSitsCommand
import uk.ac.warwick.tabula.commands.coursework.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.data.model.{Exam, Feedback, Module}
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.exams.web.controllers.ExamsController

@Controller
@RequestMapping(value = Array("/exams/admin/module/{module}/{academicYear}/exams/{exam}/upload-to-sits"))
class UploadExamFeedbackToSitsController extends ExamsController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable exam: Exam) =
		UploadFeedbackToSitsCommand(
			mandatory(module),
			mandatory(exam),
			user,
			GenerateGradesFromMarkCommand(mandatory(module), mandatory(exam))
		)

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: Appliable[Seq[Feedback]], @PathVariable module: Module, @PathVariable academicYear: AcademicYear) = {
		Mav("exams/admin/upload_to_sits",
			"isGradeValidation" -> module.adminDepartment.assignmentGradeValidation
		).crumbs(
			Breadcrumbs.Department(module.adminDepartment, academicYear),
			Breadcrumbs.Module(module, academicYear)
		)
	}

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("command") cmd: Appliable[Seq[Feedback]], @PathVariable exam: Exam) = {
		cmd.apply()
		Redirect(Routes.admin.exam(exam))
	}

}
