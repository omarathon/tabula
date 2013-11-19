package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestParam, RequestMapping}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.commands.{Appliable, CommandInternal, Unaudited, ReadOnly, ComposableCommand}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import uk.ac.warwick.tabula.attendance.commands.GroupMonitoringPointsByTerm
import uk.ac.warwick.tabula.services.{TermServiceComponent, AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent, AutowiringTermServiceComponent}
import scala.collection.JavaConverters._
import org.joda.time.DateTime

object StudentViewCommand {
	def apply(department: Department, student: StudentMember, academicYearOption: Option[AcademicYear]) =
		new StudentViewCommand(department, student, academicYearOption)
			with ComposableCommand[Map[String, Seq[(MonitoringPoint, String)]]]
			with StudentViewPermissions
			with ReadOnly with Unaudited
			with AutowiringTermServiceComponent
			with AutowiringMonitoringPointServiceComponent
			with GroupMonitoringPointsByTerm
}

class StudentViewCommand(val department: Department, val student: StudentMember, val academicYearOption: Option[AcademicYear])
	extends CommandInternal[Map[String, Seq[(MonitoringPoint, String)]]] with StudentViewCommandState {

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

trait StudentViewPermissions extends RequiresPermissionsChecking {
	this: StudentViewCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, student)
	}
}

trait StudentViewCommandState {
	def department: Department
	def student: StudentMember
	def academicYearOption: Option[AcademicYear]
	val thisAcademicYear = AcademicYear.guessByDate(DateTime.now())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)
}

@Controller
@RequestMapping(Array("/view/{department}/students/{student}"))
class StudentViewController extends AttendanceController {

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable student: StudentMember,
		@RequestParam(value="academicYear", required = false) academicYear: AcademicYear
	) = {
		StudentViewCommand(department, student, Option(academicYear))
	}

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[Map[String, Seq[(MonitoringPoint, String)]]] with StudentViewCommandState
	) = {
		Mav("home/student", "student" -> cmd.student, "pointsByTerm" -> cmd.apply()).crumbs(Breadcrumbs.ViewDepartmentStudents(cmd.department))
	}

}