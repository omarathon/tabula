package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.commands.{ReadOnly, Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpointState, MonitoringPointSet, MonitoringPoint}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.permissions.Permissions

case class AttendanceProfileInformation(
	monitoringPointsByTerm: Map[String, Seq[MonitoringPoint]],
	checkpointState: Map[String, String],
	missedCountByTerm: Map[String, Int]
)

object ProfileCommand {
	def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear) =
		new ProfileCommand(studentCourseDetails, academicYear)
		with ComposableCommand[Option[AttendanceProfileInformation]]
		with ProfilePermissions
		with ProfileCommandState
		with AutowiringTermServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with ReadOnly with Unaudited
}


abstract class ProfileCommand(val studentCourseDetails: StudentCourseDetails, val academicYear: AcademicYear)
	extends CommandInternal[Option[AttendanceProfileInformation]] with GroupMonitoringPointsByTerm with ProfileCommandState {

	self: MonitoringPointServiceComponent =>

	override def applyInternal() = {
		studentCourseDetails.studentCourseYearDetails.asScala.find(_.academicYear == academicYear).flatMap {
			studentCourseYearDetail =>
				monitoringPointService.findMonitoringPointSet(
					studentCourseDetails.route,
					studentCourseYearDetail.academicYear,
					Option(studentCourseYearDetail.yearOfStudy)
				).orElse(
					monitoringPointService.findMonitoringPointSet(studentCourseDetails.route, studentCourseYearDetail.academicYear, None)
				).map { applyForPointSet }
		}
	}

	private def applyForPointSet(pointSet: MonitoringPointSet): AttendanceProfileInformation = {
		val monitoringPointsByTerm = groupByTerm(pointSet.points.asScala, pointSet.academicYear)
		val checkpointState = monitoringPointService
			.getChecked(Seq(studentCourseDetails.student), pointSet)(studentCourseDetails.student)
			.map{	case (point, option) => point.id -> (option match {
				case Some(state) => state.dbValue
				case _ => "late"
			})
		}

		val missedCountByTerm = monitoringPointsByTerm.map{
			case (termName, points) => termName -> points.count(
				p => checkpointState(p.id).equals(MonitoringCheckpointState.MissedUnauthorised.dbValue)
			)
		}.filter{
			case (termName, count) => count > 0
		}
		
		AttendanceProfileInformation(monitoringPointsByTerm, checkpointState, missedCountByTerm)
	}
}

trait ProfilePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ProfileCommandState =>

	override def permissionsCheck(p: PermissionsChecking) = {
		p.PermissionCheck(Permissions.MonitoringPoints.View, mandatory(studentCourseDetails))
	}

}

trait ProfileCommandState {
	def studentCourseDetails: StudentCourseDetails
	def academicYear: AcademicYear

	var monitoringPointsByTerm: Map[String, Seq[MonitoringPoint]] = _
	var checkpointState: Map[String, String] = _
	var missedCountByTerm: Map[String, Int] = _
}
