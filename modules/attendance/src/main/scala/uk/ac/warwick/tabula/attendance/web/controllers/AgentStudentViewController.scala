package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestParam, RequestMapping}
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, StudentMember, StudentRelationshipType, Member}
import uk.ac.warwick.tabula.commands.{Appliable, CommandInternal, Unaudited, ReadOnly, ComposableCommand}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.tabula.attendance.commands.GroupMonitoringPointsByTerm
import uk.ac.warwick.tabula.services.{TermServiceComponent, AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent, AutowiringTermServiceComponent}
import scala.collection.JavaConverters._
import org.joda.time.DateTime

object AgentStudentViewCommand {
	def apply(agent: Member, relationshipType: StudentRelationshipType, student: StudentMember, academicYearOption: Option[AcademicYear]) =
		new AgentStudentViewCommand(agent, relationshipType, student, academicYearOption)
		with ComposableCommand[Map[StudentCourseDetails, Map[String, Seq[(MonitoringPoint, String)]]]]
		with AgentStudentViewPermissions
		with ReadOnly with Unaudited
		with AutowiringTermServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with GroupMonitoringPointsByTerm
}

class AgentStudentViewCommand(
	val agent: Member, val relationshipType: StudentRelationshipType, val student: StudentMember, val academicYearOption: Option[AcademicYear]
) extends CommandInternal[Map[StudentCourseDetails, Map[String, Seq[(MonitoringPoint, String)]]]]
	with AgentStudentViewCommandState {

	this: TermServiceComponent with MonitoringPointServiceComponent with GroupMonitoringPointsByTerm =>

	def applyInternal() = {
		val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(new DateTime(), academicYear)
		student.studentCourseDetails.asScala.filter(_.hasCurrentEnrolment).map(scd =>
			scd -> {
				scd.studentCourseYearDetails.asScala.find(_.academicYear == academicYear).map(scyd => {
					monitoringPointService.findMonitoringPointSet(scd.route, academicYear, Option(scyd.yearOfStudy)).orElse(
						monitoringPointService.findMonitoringPointSet(scd.route, academicYear, None)
					).map(pointSet => {
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
					}).getOrElse(Map())
				}).getOrElse(Map())
			}
		).toMap
	}

}

trait AgentStudentViewPermissions extends RequiresPermissionsChecking {
	this: AgentStudentViewCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(p.mandatory(relationshipType)), agent)
	}
}

trait AgentStudentViewCommandState {
	def agent: Member
	def relationshipType: StudentRelationshipType
	def student: StudentMember
	def academicYearOption: Option[AcademicYear]
	val thisAcademicYear = AcademicYear.guessByDate(DateTime.now())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)
}

@Controller
@RequestMapping(Array("/agent/{relationshipType}/{member}"))
class AgentStudentViewController extends AttendanceController {

	@ModelAttribute("command")
	def command(
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable member: Member,
		@RequestParam(value="academicYear", required = false) academicYear: AcademicYear
	) = {
		member match {
			case (student: StudentMember) => AgentStudentViewCommand(currentMember, relationshipType, student, Option(academicYear))
			case _ => throw new IllegalArgumentException()
		}
	}

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[Map[StudentCourseDetails, Map[String, Seq[(MonitoringPoint, String)]]]] with AgentStudentViewCommandState
	) = {
		Mav("agent/student", "student" -> cmd.student, "courseMap" -> cmd.apply())
	}

}
