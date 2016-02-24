package uk.ac.warwick.tabula.commands.attendance.profile.old

import org.springframework.beans.factory.annotation.Value
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.old.{BuildStudentPointsData, StudentPointsData}
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, TaskBenchmarking, Unaudited}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

case class OldAttendanceProfileInformation(
	pointsData: StudentPointsData,
	missedCountByTerm: Map[String, Int],
	nonReportedTerms: Seq[String]
)

object OldProfileCommand {
	def apply(student: StudentMember, academicYear: AcademicYear) =
		new OldProfileCommand(student, academicYear)
		with ComposableCommand[OldAttendanceProfileInformation]
		with ProfilePermissions
		with ProfileCommandState
		with AutowiringTermServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with AutowiringUserLookupComponent
		with ReadOnly with Unaudited
}


abstract class OldProfileCommand(val student: StudentMember, val academicYear: AcademicYear)
	extends CommandInternal[OldAttendanceProfileInformation] with TaskBenchmarking with BuildStudentPointsData with ProfileCommandState {


	override def applyInternal() = {
		val pointsData = benchmarkTask("Build data") { buildData(Seq(student), academicYear).head }

		val missedCountByTerm = pointsData.pointsByTerm.map{
			case (termName, pointMap) => termName -> pointMap.count{
				case (point, checkpointData) => checkpointData.state == AttendanceState.MissedUnauthorised.dbValue
			}
		}.filter{
			case (termName, count) => count > 0
		}

		val nonReportedTerms = monitoringPointService.findNonReportedTerms(Seq(student), academicYear)

		OldAttendanceProfileInformation(pointsData, missedCountByTerm, nonReportedTerms)
	}
}

trait ProfilePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods with Logging {

	self: ProfileCommandState =>

	override def permissionsCheck(p: PermissionsChecking) = {
		p.PermissionCheck(Permissions.MonitoringPoints.View, mandatory(student))
	}

}

trait ProfileCommandState {
	def student: StudentMember
	def academicYear: AcademicYear

	@Value("${tabula.yearZero}") var yearZero: Int = _
}
