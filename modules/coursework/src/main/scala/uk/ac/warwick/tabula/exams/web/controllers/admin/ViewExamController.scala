package uk.ac.warwick.tabula.exams.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.exams.commands.{ViewExamCommandResult, ViewExamCommand}
import uk.ac.warwick.tabula.exams.web.controllers.ExamsController

@Controller
@RequestMapping(Array("/exams/admin/module/{module}/{academicYear}/exams/{exam}"))
class ViewExamController extends ExamsController {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable academicYear: AcademicYear, @PathVariable exam: Exam) =
		ViewExamCommand(mandatory(module), mandatory(academicYear), mandatory(exam))

	@RequestMapping(method = Array(HEAD, GET))
	def home(
		@ModelAttribute("command") cmd: Appliable[ViewExamCommandResult],
		@PathVariable module: Module,
		@PathVariable academicYear: AcademicYear
	) = {
		val result = cmd.apply()
		Mav("exams/admin/view",
			"students" -> result.students,
			"feedbackMap" -> result.feedbackMap,
			"sitsStatusMap" -> result.sitsStatusMap
		).crumbs(
			Breadcrumbs.Department(module.adminDepartment, academicYear),
			Breadcrumbs.Module(module, academicYear)
		)
	}

}
