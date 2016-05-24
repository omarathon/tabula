package uk.ac.warwick.tabula.web.controllers.attendance.agent.old

import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.attendance.agent.old.AgentViewCommand
import uk.ac.warwick.tabula.commands.attendance.old.{GroupedMonitoringPoint, StudentPointsData}
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

@RequestMapping(Array("/attendance/agent/{relationshipType}/2013"))
class AgentViewController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable relationshipType: StudentRelationshipType) =
		AgentViewCommand(currentMember, relationshipType, Option(AcademicYear(2013)))

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[(Seq[StudentPointsData], Map[String, Seq[GroupedMonitoringPoint]])]) = {
		val (students, groupedPoints) = cmd.apply()

		Mav("attendance/agent/old/students",
			"students" -> students,
			"canRecordAny" -> securityService.canForAny(user, Permissions.MonitoringPoints.Record, students.map(_.student)),
			"necessaryTerms" -> students.flatMap{ data => data.pointsByTerm.keySet }.distinct,
			"groupedPoints" -> groupedPoints,
			"department" -> currentMember.homeDepartment
		)
	}

}
