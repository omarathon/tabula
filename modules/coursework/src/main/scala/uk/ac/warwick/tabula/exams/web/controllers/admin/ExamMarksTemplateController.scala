package uk.ac.warwick.tabula.exams.web.controllers.admin

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.coursework.commands.feedback.MarksTemplateCommand._
import uk.ac.warwick.tabula.data.model.{Exam, Module}
import uk.ac.warwick.tabula.exams.commands.GenerateExamMarksTemplateCommand
import uk.ac.warwick.tabula.exams.web.controllers.ExamsController
import uk.ac.warwick.tabula.services.AssessmentMembershipService
import uk.ac.warwick.tabula.web.views.ExcelView

@Controller
@RequestMapping(value = Array("/exams/admin/module/{module}/{academicYear}/exams/{exam}/marks-template"))
class ExamMarksTemplateController extends ExamsController {

	var examMembershipService = Wire[AssessmentMembershipService]

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @PathVariable exam: Exam) =
		GenerateExamMarksTemplateCommand(
			mandatory(module),
			mandatory(exam),
			examMembershipService.determineMembershipUsersWithOrder(exam)
		)

	@RequestMapping(method = Array(HEAD, GET))
	def generateMarksTemplate(@ModelAttribute("command") cmd: Appliable[XSSFWorkbook], @PathVariable exam: Exam) = {
		new ExcelView(safeAssessmentName(exam) + " marks.xlsx", cmd.apply())
	}
}
