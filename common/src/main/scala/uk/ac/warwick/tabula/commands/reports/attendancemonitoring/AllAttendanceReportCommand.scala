package uk.ac.warwick.tabula.commands.reports.attendancemonitoring

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.commands.reports.{ReportCommandRequest, ReportCommandRequestValidation, ReportCommandState, ReportPermissions}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}

object AllAttendanceReportCommand {
  type ResultType = AllAttendanceReportCommandResult
  type CommandType = Appliable[AllAttendanceReportCommandResult] with ReportCommandRequestValidation

  def apply(
    department: Department,
    academicYear: AcademicYear,
    filter: AllAttendanceReportCommandResult => AllAttendanceReportCommandResult
  ): CommandType =
    new AllAttendanceReportCommandInternal(department, academicYear, filter)
      with AutowiringProfileServiceComponent
      with AutowiringAttendanceMonitoringServiceComponent
      with ComposableCommand[AllAttendanceReportCommandResult]
      with ReportPermissions
      with AllAttendanceReportCommandState
      with ReportCommandRequest
      with ReportCommandRequestValidation
      with ReadOnly with Unaudited
}

case class AllAttendanceReportCommandResult(
  studentDataMap: Map[AttendanceMonitoringStudentData, Map[AttendanceMonitoringPoint, AttendanceState]],
  reportRangeStartDate: LocalDate,
  reportRangeEndDate: LocalDate
)

class AllAttendanceReportCommandInternal(
  val department: Department,
  val academicYear: AcademicYear,
  val filter: AllAttendanceReportCommandResult => AllAttendanceReportCommandResult
)
  extends CommandInternal[AllAttendanceReportCommandResult] with TaskBenchmarking {

  self: ProfileServiceComponent with AttendanceMonitoringServiceComponent with ReportCommandRequest =>

  override def applyInternal(): AllAttendanceReportCommandResult = {
    val allStudentData = benchmarkTask("allStudentData") {
      profileService.findAllStudentDataByRestrictionsInAffiliatedDepartments(department, Seq(), academicYear)
    }
    val studentPointMap = benchmarkTask("studentPointMap") {
      allStudentData.map(studentData => studentData ->
        attendanceMonitoringService.listStudentsPoints(studentData, department, academicYear)
          .filter(p => (p.startDate.isEqual(startDate) || p.startDate.isAfter(startDate)) && (p.startDate.isEqual(endDate) || p.startDate.isBefore(endDate)))
      ).filter(_._2.nonEmpty)
    }.toMap
    val checkpointMap = benchmarkTask("checkpointMap") {
      attendanceMonitoringService.getAllCheckpointData(studentPointMap.values.flatten.toSeq.distinct).groupBy(_.point)
    }

    val result: Map[AttendanceMonitoringStudentData, Map[AttendanceMonitoringPoint, AttendanceState]] = benchmarkTask("result") {
      studentPointMap.map { case (studentData, points) =>
        studentData -> points.map(point => point -> checkpointMap.get(point).flatMap(
          checkpoints => checkpoints.find(_.universityId == studentData.universityId).map(_.state)).getOrElse(AttendanceState.NotRecorded)
        ).toMap
      }
    }
    filter(AllAttendanceReportCommandResult(result, startDate, endDate))
  }

}

trait AllAttendanceReportCommandState extends ReportCommandState {
}
