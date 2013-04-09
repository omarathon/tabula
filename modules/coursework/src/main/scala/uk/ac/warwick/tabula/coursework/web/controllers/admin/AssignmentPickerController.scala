package uk.ac.warwick.tabula.coursework.web.controllers.admin

import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.web.bind.annotation.RequestMethod
import com.fasterxml.jackson.databind.ObjectMapper
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.spring.Wire
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.permissions._

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/picker"))
class AssignmentPickerController extends CourseworkController {
	@Autowired var json: ObjectMapper = _
	
	@ModelAttribute def command(@PathVariable("module") module: Module) = new AssignmentPickerCommand(module)

	@RequestMapping
	def submit(cmd: AssignmentPickerCommand) = {
		val assignmentsJson: JList[Map[String, Object]] = toJson(cmd.apply())

		new JSONView(assignmentsJson)
	}

	def toJson(assignments: Seq[Assignment]) = {

		def assignmentToJson(assignment: Assignment) = Map[String, String](
			"name" -> assignment.name,
			"id" -> assignment.id,
			"moduleName" -> assignment.module.name,
			"moduleCode" -> assignment.module.code)

		val assignmentsJson = assignments.map(assignmentToJson(_))
		assignmentsJson
	}

}

class AssignmentPickerCommand(module: Module) extends Command[Seq[Assignment]] with ReadOnly with Unaudited {
	PermissionCheck(Permissions.Assignment.Read, module)
	
	var assignmentService = Wire.auto[AssignmentService]
	
	var searchTerm: String = ""
		
	def applyInternal() = assignmentService.getAssignmentsByName(searchTerm, module.department)
}
