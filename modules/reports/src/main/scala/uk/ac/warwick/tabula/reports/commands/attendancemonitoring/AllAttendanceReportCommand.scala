package uk.ac.warwick.tabula.reports.commands.attendancemonitoring

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.reports.commands.{ReportCommandState, ReportPermissions}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}

object AllAttendanceReportCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new AllAttendanceReportCommandInternal(department, academicYear)
			with AutowiringProfileServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with ComposableCommand[Map[AttendanceMonitoringStudentData, Map[AttendanceMonitoringPoint, AttendanceState]]]
			with ReportPermissions
			with AllAttendanceReportCommandState
			with ReadOnly with Unaudited
}

class AllAttendanceReportCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[Map[AttendanceMonitoringStudentData, Map[AttendanceMonitoringPoint, AttendanceState]]] with TaskBenchmarking {

	self: ProfileServiceComponent with AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		val allStudentData = benchmarkTask("allStudentData") {
			profileService.findAllStudentDataByRestrictionsInAffiliatedDepartments(department, Seq(), academicYear)
		}
		val studentPointMap = benchmarkTask("studentPointMap") {
			allStudentData.map(studentData => studentData -> attendanceMonitoringService.listStudentsPoints(studentData, department, academicYear))
				.filter(_._2.nonEmpty)
		}.toMap
		val checkpointMap = benchmarkTask("checkpointMap") {
			attendanceMonitoringService.getAllCheckpointData(studentPointMap.values.flatten.toSeq.distinct).groupBy(_.point)
		}

		benchmarkTask("result") {
			studentPointMap.map { case (studentData, points) =>
				studentData -> points.map(point => point -> checkpointMap.get(point).flatMap(
					checkpoints => checkpoints.find(_.universityId == studentData.universityId).map(_.state)).getOrElse(AttendanceState.NotRecorded)
				).toMap
			}
		}

	}

}

trait AllAttendanceReportCommandState extends ReportCommandState {
}
