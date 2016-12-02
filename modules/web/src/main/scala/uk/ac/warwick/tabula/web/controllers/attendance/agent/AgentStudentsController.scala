package uk.ac.warwick.tabula.web.controllers.attendance.agent

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.agent.{AgentStudentsCommand, AgentStudentsCommandResult}
import uk.ac.warwick.tabula.web.controllers.attendance.{AttendanceController, HasMonthNames}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/attendance/agent/{relationshipType}/{academicYear}"))
class AgentStudentsController extends AttendanceController with HasMonthNames {

	@ModelAttribute("command")
	def command(@PathVariable relationshipType: StudentRelationshipType, @PathVariable academicYear: AcademicYear) =
		AgentStudentsCommand(mandatory(relationshipType), mandatory(academicYear), mandatory(currentMember))

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[AgentStudentsCommandResult],
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		val result = cmd.apply()
		Mav("attendance/agent/list",
			"studentAttendance" -> result.studentAttendance,
			"visiblePeriods" -> result.studentAttendance.results.flatMap(_.groupedPointCheckpointPairs.map(_._1)).distinct,
			"department" -> currentMember.homeDepartment,
			"canRecordAny" -> securityService.canForAny(user, Permissions.MonitoringPoints.Record, result.studentAttendance.results.map(_.student)),
			"groupedPoints" -> result.groupedPoints
		).crumbs(
			Breadcrumbs.Agent.Relationship(relationshipType)
		)
	}

}
