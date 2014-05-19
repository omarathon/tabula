package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, Department}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.attendance.commands.{ViewAgentsResult, ViewAgentsCommand}

@Controller
@RequestMapping(Array("/view/{department}/2013/agents/{relationshipType}"))
class ViewAgentsController extends AttendanceController {

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable relationshipType: StudentRelationshipType
	) =
		ViewAgentsCommand(department, relationshipType, Option(AcademicYear(2013)))

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Seq[ViewAgentsResult]], @PathVariable department: Department) = {
		Mav("home/agents", "agents" -> cmd.apply()).crumbs(Breadcrumbs.Old.ViewDepartment(department))
	}

}
