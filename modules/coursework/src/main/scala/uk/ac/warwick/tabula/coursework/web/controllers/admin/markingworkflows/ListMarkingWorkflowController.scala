package uk.ac.warwick.tabula.coursework.web.controllers.admin.markingworkflows

import scala.collection.JavaConversions._
import org.springframework.web.bind.annotation._
import org.springframework.stereotype.Controller
import org.hibernate.criterion.Restrictions
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.MarkingWorkflow
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
	
	@ModelAttribute("command") def command(@PathVariable("department") department: Department) = new Form(department)
	
	@RequestMapping
	def list(@ModelAttribute("command") form: Form): Mav = {
		Mav("admin/markingworkflows/list",
		    "markingWorkflowInfo" -> form.apply())
		    .crumbsList(getCrumbs(form))
	}
	
	def getCrumbs(form: Form) = Seq (
		Breadcrumbs.Department(form.department)
	)
	
}

object ListMarkingWorkflowController {
	class Form(val department: Department) extends Command[Seq[Map[String, Any]]] with ReadOnly with Unaudited with Daoisms {
		PermissionCheck(Permissions.MarkingWorkflow.Read, department)
	
		var dao = Wire.auto[MarkingWorkflowDao]

		def applyInternal() = {
			val markingWorkflows = session.newCriteria[MarkingWorkflow]
				.add(is("department", department))
				.list
		  
			for (markingWorkflow <- markingWorkflows) yield {
				val assignments = dao.getAssignmentsUsingMarkingWorkflow(markingWorkflow)
				Map(
					"markingWorkflow" -> markingWorkflow,
					"assignmentCount" -> assignments.size)
			}
		}
	}
}