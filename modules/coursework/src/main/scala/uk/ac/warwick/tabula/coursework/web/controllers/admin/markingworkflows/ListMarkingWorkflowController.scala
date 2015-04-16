package uk.ac.warwick.tabula.coursework.web.controllers.admin.markingworkflows

import org.springframework.web.bind.annotation._
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.exams.web.controllers.ExamsController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.MarkingWorkflowDao
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.permissions._

@Controller
@RequestMapping(value=Array("/admin/department/{department}/markingworkflows"))
class ListMarkingWorkflowController extends CourseworkController {
	import ListMarkingWorkflowController._
	
	@ModelAttribute("command") def command(@PathVariable("department") department: Department) = new Form(department, isExam = false)
	
	@RequestMapping
	def list(@ModelAttribute("command") form: Form): Mav = {
		Mav("admin/markingworkflows/list",
		    "markingWorkflowInfo" -> form.apply(),
				"isExams" -> false)
		    .crumbsList(getCrumbs(form))
	}
	
	def getCrumbs(form: Form) = Seq (
		Breadcrumbs.Department(form.department)
	)
	
}

@Controller
@RequestMapping(value=Array("/exams/admin/department/{department}/markingworkflows"))
class ExamListMarkingWorkflowController extends ExamsController {

	import ListMarkingWorkflowController._

	@ModelAttribute("command") def command(@PathVariable("department") department: Department) = new Form(department, isExam = true)

	@RequestMapping
	def list(@ModelAttribute("command") form: Form): Mav = {
		Mav("admin/markingworkflows/list",
			"markingWorkflowInfo" -> form.apply(),
			"isExams" -> true)
	}

}

object ListMarkingWorkflowController {
	class Form(val department: Department, val isExam: Boolean) extends Command[Seq[Map[String, Any]]] with ReadOnly with Unaudited {
		PermissionCheck(Permissions.MarkingWorkflow.Read, department)
	
		var dao = Wire.auto[MarkingWorkflowDao]

		def applyInternal() = {

			val validWorkflows = if (isExam) department.markingWorkflows.filter(_.validForExams) else department.markingWorkflows
			validWorkflows.map { markingWorkflow =>
				val assignments = dao.getAssignmentsUsingMarkingWorkflow(markingWorkflow)
				Map(
					"markingWorkflow" -> markingWorkflow,
					"assignmentCount" -> assignments.size)
			}
		}
	}
}