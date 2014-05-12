package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Department, Member, StudentRelationshipType}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.{StudentPointsData, ViewAgentsStudentsCommand}

@Controller
@RequestMapping(Array("/view/{department}/2013/agents/{relationshipType}/{agent}"))
class ViewAgentsStudentsController extends AttendanceController {

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable agent: Member,
		@PathVariable relationshipType: StudentRelationshipType
	) =
		ViewAgentsStudentsCommand(department, agent, relationshipType, Option(AcademicYear(2013)))

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[Seq[StudentPointsData]],
		@PathVariable department: Department,
		@PathVariable relationshipType: StudentRelationshipType
	) = {
		val students = cmd.apply()
		Mav("home/agents_students",
			"students" -> students,
			"necessaryTerms" -> students.flatMap{ data => data.pointsByTerm.keySet }.distinct
		).crumbs(Breadcrumbs.ViewDepartment(department), Breadcrumbs.ViewDepartmentAgents(department, relationshipType))
	}

}
