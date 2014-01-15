package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.commands.{TaskBenchmarking, ReadOnly, Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.permissions.Permissions

case class AttendanceProfileInformation(
	pointsData: StudentPointsData,
	missedCountByTerm: Map[String, Int]
)

object ProfileCommand {
	def apply(student: StudentMember, academicYear: AcademicYear) =
		new ProfileCommand(student, academicYear)
		with ComposableCommand[AttendanceProfileInformation]
		with ProfilePermissions
		with ProfileCommandState
		with AutowiringTermServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with AutowiringProfileServiceComponent
		with ReadOnly with Unaudited
}


abstract class ProfileCommand(val student: StudentMember, val academicYear: AcademicYear)
	extends CommandInternal[AttendanceProfileInformation] with TaskBenchmarking with BuildStudentPointsData with ProfileCommandState {

	override def applyInternal() = {
		val pointsData = benchmarkTask("Build data") { buildData(Seq(student), academicYear).head }

		val missedCountByTerm = pointsData.pointsByTerm.map{
			case (termName, pointMap) => termName -> pointMap.count{
				case (point, checkpointData) => checkpointData.state == AttendanceState.MissedUnauthorised.dbValue
			}
		}.filter{
			case (termName, count) => count > 0
		}

		AttendanceProfileInformation(pointsData, missedCountByTerm)
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
