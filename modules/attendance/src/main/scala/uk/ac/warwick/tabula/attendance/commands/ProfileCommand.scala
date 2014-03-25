package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.commands.{TaskBenchmarking, ReadOnly, Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{ItemNotFoundException, AcademicYear}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.beans.factory.annotation.Value
import uk.ac.warwick.tabula.helpers.Logging

case class AttendanceProfileInformation(
	pointsData: StudentPointsData,
	missedCountByTerm: Map[String, Int],
	nonReportedTerms: Seq[String]
)

object ProfileCommand {
	def apply(student: StudentMember, academicYear: AcademicYear) =
		new ProfileCommand(student, academicYear)
		with ComposableCommand[AttendanceProfileInformation]
		with ProfilePermissions
		with ProfileCommandState
		with AutowiringTermServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with AutowiringUserLookupComponent
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

		val nonReportedTerms = monitoringPointService.findNonReportedTerms(Seq(student), academicYear)

		AttendanceProfileInformation(pointsData, missedCountByTerm, nonReportedTerms)
	}
}

trait ProfilePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods with Logging {

	self: ProfileCommandState =>

	override def permissionsCheck(p: PermissionsChecking) = {
		// TODO there's probably a better place to do this
		if (academicYear.startYear < yearZero) {
			logger.info("Not showing attendance monitoring profile for year less than year zero: " + academicYear)
			throw new ItemNotFoundException
		}

		p.PermissionCheck(Permissions.MonitoringPoints.View, mandatory(student))
	}

}

trait ProfileCommandState {
	def student: StudentMember
	def academicYear: AcademicYear

	@Value("${tabula.yearZero}") var yearZero: Int = _
}
