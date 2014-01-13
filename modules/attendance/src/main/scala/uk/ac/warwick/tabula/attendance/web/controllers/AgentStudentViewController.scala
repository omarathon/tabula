package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestParam, RequestMapping}
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.commands.{TaskBenchmarking, Appliable, CommandInternal, Unaudited, ReadOnly, ComposableCommand}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.{StudentPointsData, BuildStudentPointsData, GroupMonitoringPointsByTerm}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringMonitoringPointServiceComponent, AutowiringTermServiceComponent}
import org.joda.time.DateTime

object AgentStudentViewCommand {
	def apply(student: StudentMember, academicYearOption: Option[AcademicYear]) =
		new AgentStudentViewCommand(student, academicYearOption)
		with ComposableCommand[StudentPointsData]
		with AgentStudentViewPermissions
		with ReadOnly with Unaudited
		with AutowiringTermServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with AutowiringProfileServiceComponent
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
	val thisAcademicYear = AcademicYear.guessByDate(DateTime.now())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)
}

@Controller
@RequestMapping(Array("/agent/{relationshipType}/{student}"))
class AgentStudentViewController extends AttendanceController {

	@ModelAttribute("command")
	def command(
		@PathVariable student: StudentMember,
		@RequestParam(value="academicYear", required = false) academicYear: AcademicYear
	) = AgentStudentViewCommand(student, Option(academicYear))

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[StudentPointsData],
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable student: StudentMember
	) = {
		Mav("agent/student",
			"student" -> student,
			"relationshipType" -> relationshipType,
			"pointsByTerm" -> cmd.apply().pointsByTerm
		).crumbs(Breadcrumbs.Agent(relationshipType))
	}

}
