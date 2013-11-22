package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, Department}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.attendance.commands.{ViewAgentsResult, ViewAgentsCommand}

@Controller
@RequestMapping(Array("/view/{department}/agents/{relationshipType}"))
class ViewAgentsController extends AttendanceController {

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable relationshipType: StudentRelationshipType,
		@RequestParam(value="academicYear", required=false) academicYear: AcademicYear
	) =
		ViewAgentsCommand(department, relationshipType, Option(academicYear))

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Seq[ViewAgentsResult]], @PathVariable department: Department) = {
		Mav("home/agents", "agents" -> cmd.apply()).crumbs(Breadcrumbs.ViewDepartment(department))
	}

}
