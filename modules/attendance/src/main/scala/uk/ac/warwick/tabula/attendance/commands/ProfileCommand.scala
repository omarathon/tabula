package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.commands.{ReadOnly, Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceState, MonitoringPointSet, MonitoringPoint}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.permissions.Permissions

case class AttendanceProfileInformation(
	monitoringPointsByTerm: Map[String, Seq[MonitoringPoint]],
	checkpointState: Map[String, String],
	missedCountByTerm: Map[String, Int]
)

object ProfileCommand {
	def apply(student: StudentMember, academicYear: AcademicYear) =
		new ProfileCommand(student, academicYear)
		with ComposableCommand[Option[AttendanceProfileInformation]]
		with ProfilePermissions
		with ProfileCommandState
		with AutowiringTermServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with ReadOnly with Unaudited
}


abstract class ProfileCommand(val student: StudentMember, val academicYear: AcademicYear)
	extends CommandInternal[Option[AttendanceProfileInformation]] with GroupMonitoringPointsByTerm with ProfileCommandState {

	self: MonitoringPointServiceComponent =>

	override def applyInternal() = {
		monitoringPointService.getPointSetForStudent(student, academicYear).map { applyForPointSet }
	}

	private def applyForPointSet(pointSet: MonitoringPointSet): AttendanceProfileInformation = {
		val monitoringPointsByTerm = groupByTerm(pointSet.points.asScala, pointSet.academicYear)
		val checkpointState = monitoringPointService
			.getChecked(Seq(student), pointSet)(student)
			.map{	case (point, option) => point.id -> (option match {
				case Some(state) => state.dbValue
				case _ => "late"
			})
		}

		val missedCountByTerm = monitoringPointsByTerm.map{
			case (termName, points) => termName -> points.count(
				p => checkpointState(p.id).equals(AttendanceState.MissedUnauthorised.dbValue)
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
		p.PermissionCheck(Permissions.MonitoringPoints.View, mandatory(student))
	}

}

trait ProfileCommandState {
	def student: StudentMember
	def academicYear: AcademicYear
}
