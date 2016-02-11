package uk.ac.warwick.tabula.web.controllers.attendance.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.view.{ViewAgentsCommandResult, ViewAgentsCommand}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.{StudentRelationshipType, Department}

@Controller
@RequestMapping(Array("/attendance/view/{department}/{academicYear}/agents/{relationshipType}"))
class ViewAgentsController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable relationshipType: StudentRelationshipType) =
		ViewAgentsCommand(mandatory(department), mandatory(academicYear), mandatory(relationshipType))

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[Seq[ViewAgentsCommandResult]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@PathVariable relationshipType: StudentRelationshipType
	) = {
		val agents = cmd.apply()
		Mav("attendance/view/agents",
			"agents" -> agents,
			"agentsEmails" -> agents.flatMap(a => Option(a.agentMember)).flatMap(_.email.maybeText)
		).crumbs(
			Breadcrumbs.View.Home,
			Breadcrumbs.View.Department(department),
			Breadcrumbs.View.DepartmentForYear(department, academicYear)
		)
	}

}
