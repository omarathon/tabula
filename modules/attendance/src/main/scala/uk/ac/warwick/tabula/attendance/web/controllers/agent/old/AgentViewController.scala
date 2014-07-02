package uk.ac.warwick.tabula.attendance.web.controllers.agent.old

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.agent.old.AgentViewCommand
import uk.ac.warwick.tabula.attendance.commands.old.{GroupedMonitoringPoint, StudentPointsData}
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

@Controller
@RequestMapping(Array("/agent/{relationshipType}/2013"))
class AgentViewController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable relationshipType: StudentRelationshipType) =
		AgentViewCommand(currentMember, relationshipType, Option(AcademicYear(2013)))

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[(Seq[StudentPointsData], Map[String, Seq[GroupedMonitoringPoint]])]) = {
		val (students, groupedPoints) = cmd.apply()
		Mav("agent/old/students",
			"students" -> students,
			"necessaryTerms" -> students.flatMap{ data => data.pointsByTerm.keySet }.distinct,
			"groupedPoints" -> groupedPoints,
			"department" -> currentMember.homeDepartment
		)
	}

}
