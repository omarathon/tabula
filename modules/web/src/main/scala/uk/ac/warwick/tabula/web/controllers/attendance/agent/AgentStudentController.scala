package uk.ac.warwick.tabula.web.controllers.attendance.agent

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.agent.AgentStudentCommand
import uk.ac.warwick.tabula.web.controllers.attendance.{AttendanceController, HasMonthNames}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/attendance/agent/{relationshipType}/{academicYear}/{student}"))
class AgentStudentController extends AttendanceController with HasMonthNames {

	@ModelAttribute("command")
	def command(
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable academicYear: AcademicYear,
		@PathVariable student: StudentMember
	) = AgentStudentCommand(mandatory(relationshipType), mandatory(academicYear), mandatory(student))

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]]],
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		Mav("attendance/agent/student",
			"groupedPointMap" -> cmd.apply(),
			"department" -> currentMember.homeDepartment
		).crumbs(
			Breadcrumbs.Agent.Relationship(relationshipType),
			Breadcrumbs.Agent.RelationshipForYear(relationshipType, academicYear)
		)
	}

}
