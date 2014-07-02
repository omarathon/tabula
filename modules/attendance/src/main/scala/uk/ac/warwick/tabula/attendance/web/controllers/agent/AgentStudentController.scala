package uk.ac.warwick.tabula.attendance.web.controllers.agent

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.agent.AgentStudentCommand
import uk.ac.warwick.tabula.attendance.web.controllers.{HasMonthNames, AttendanceController}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationshipType}

@Controller
@RequestMapping(Array("/agent/{relationshipType}/{academicYear}/{student}"))
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
	) = {
		Mav("agent/student",
			"groupedPointMap" -> cmd.apply(),
			"department" -> currentMember.homeDepartment
		).crumbs(
			Breadcrumbs.Agent.Relationship(relationshipType),
			Breadcrumbs.Agent.RelationshipForYear(relationshipType, academicYear)
		)
	}

}
