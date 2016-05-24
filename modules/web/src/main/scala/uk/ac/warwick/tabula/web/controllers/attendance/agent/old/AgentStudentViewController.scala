package uk.ac.warwick.tabula.web.controllers.attendance.agent.old

import org.joda.time.DateTime
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.old.{BuildStudentPointsData, GroupMonitoringPointsByTerm, StudentPointsData}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointServiceComponent, AutowiringTermServiceComponent, AutowiringUserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

object AgentStudentViewCommand {
	def apply(student: StudentMember, academicYearOption: Option[AcademicYear]) =
		new AgentStudentViewCommand(student, academicYearOption)
		with ComposableCommand[StudentPointsData]
		with AgentStudentViewPermissions
		with ReadOnly with Unaudited
		with AutowiringTermServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with AutowiringUserLookupComponent
		with GroupMonitoringPointsByTerm
}

abstract class AgentStudentViewCommand(val student: StudentMember, val academicYearOption: Option[AcademicYear])
	extends CommandInternal[StudentPointsData] with AgentStudentViewCommandState with TaskBenchmarking with BuildStudentPointsData {

	def applyInternal() = {
		benchmarkTask("Build data") { buildData(Seq(student), academicYear).head }
	}

}

trait AgentStudentViewPermissions extends RequiresPermissionsChecking {
	this: AgentStudentViewCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, student)
	}
}

trait AgentStudentViewCommandState {
	def student: StudentMember
	def academicYearOption: Option[AcademicYear]
	val thisAcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)
}

@RequestMapping(Array("/attendance/agent/{relationshipType}/2013/{student}"))
class AgentStudentViewController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable student: StudentMember) =
		AgentStudentViewCommand(student, Option(AcademicYear(2013)))

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[StudentPointsData],
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable student: StudentMember
	) = {
		Mav("attendance/agent/old/student",
			"student" -> student,
			"relationshipType" -> relationshipType,
			"pointsByTerm" -> cmd.apply().pointsByTerm
		).crumbs(Breadcrumbs.Old.Agent(relationshipType))
	}

}
