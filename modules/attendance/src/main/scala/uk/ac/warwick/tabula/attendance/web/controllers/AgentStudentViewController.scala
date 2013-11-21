package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestParam, RequestMapping}
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.commands.{Appliable, CommandInternal, Unaudited, ReadOnly, ComposableCommand}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import uk.ac.warwick.tabula.attendance.commands.GroupMonitoringPointsByTerm
import uk.ac.warwick.tabula.services.{TermServiceComponent, AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent, AutowiringTermServiceComponent}
import scala.collection.JavaConverters._
import org.joda.time.DateTime

object AgentStudentViewCommand {
	def apply(student: StudentMember, academicYearOption: Option[AcademicYear]) =
		new AgentStudentViewCommand(student, academicYearOption)
		with ComposableCommand[Map[String, Seq[(MonitoringPoint, String)]]]
		with AgentStudentViewPermissions
		with ReadOnly with Unaudited
		with AutowiringTermServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with GroupMonitoringPointsByTerm
}

class AgentStudentViewCommand(val student: StudentMember, val academicYearOption: Option[AcademicYear])
	extends CommandInternal[Map[String, Seq[(MonitoringPoint, String)]]] with AgentStudentViewCommandState {

	this: TermServiceComponent with MonitoringPointServiceComponent with GroupMonitoringPointsByTerm =>

	def applyInternal() = {
		val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(new DateTime(), academicYear)
		monitoringPointService.getPointSetForStudent(student, academicYear) match {
			case Some(set) => checkpointStateStrings(set, currentAcademicWeek)
			case None => Map()
		}
	}

	private def checkpointStateStrings(pointSet: MonitoringPointSet, currentAcademicWeek: Int) = {
		val checkedForStudent = monitoringPointService.getChecked(Seq(student), pointSet)(student)
		groupByTerm(pointSet.points.asScala, academicYear).map{case (termName, points) =>
			termName -> points.map(point =>
				point -> (checkedForStudent(point) match {
					case Some(state) => state.dbValue
					case _ =>
						if (point.isLate(currentAcademicWeek))
							"late"
						else
							""
				})
			)
		}
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
		@ModelAttribute("command") cmd: Appliable[Map[String, Seq[(MonitoringPoint, String)]]],
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable student: StudentMember
	) = {
		Mav("agent/student",
			"student" -> student,
			"relationshipType" -> relationshipType,
			"pointsByTerm" -> cmd.apply()
		).crumbs(Breadcrumbs.Agent(relationshipType))
	}

}
