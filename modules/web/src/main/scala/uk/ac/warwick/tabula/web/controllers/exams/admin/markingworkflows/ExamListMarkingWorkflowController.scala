package uk.ac.warwick.tabula.web.controllers.exams.admin.markingworkflows

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.markingworkflows.{ListMarkingWorkflowCommandResult, ListMarkingWorkflowCommand}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController

@Controller
@RequestMapping(value=Array("/exams/admin/department/{department}/markingworkflows"))
class ExamListMarkingWorkflowController extends ExamsController {

	@ModelAttribute("command")
	def command(@PathVariable("department") department: Department) = ListMarkingWorkflowCommand(department, isExam = true)

	@RequestMapping
	def list(@ModelAttribute("command") cmd: Appliable[Seq[ListMarkingWorkflowCommandResult]]): Mav = {
		Mav("exams/admin/markingworkflows/list",
			"markingWorkflowInfo" -> cmd.apply(),
			"isExams" -> true
		)
	}

}
