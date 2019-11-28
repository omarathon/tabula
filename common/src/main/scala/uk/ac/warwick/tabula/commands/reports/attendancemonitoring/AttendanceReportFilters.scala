package uk.ac.warwick.tabula.commands.reports.attendancemonitoring

import uk.ac.warwick.tabula.data.model.attendance.AttendanceState

object AttendanceReportFilters {

  def identity(result: AllAttendanceReportCommandResult): AllAttendanceReportCommandResult = result

  def unrecorded(result: AllAttendanceReportCommandResult): AllAttendanceReportCommandResult = {
    val unrecordedPoints = result.studentDataMap.flatMap { case (studentData, pointMap) =>
      pointMap.filter { case (point, state) =>
        point.endDate.toDateTimeAtStartOfDay.isBeforeNow && state == AttendanceState.NotRecorded
      }.keySet
    }.toSeq
    AllAttendanceReportCommandResult(result.studentDataMap.map { case (studentData, pointMap) =>
      studentData -> pointMap.view.filterKeys(unrecordedPoints.contains).toMap
    }.filter { case (_, pointMap) => pointMap.nonEmpty }
    ,result.reportRangeStartDate, result.reportRangeEndDate)
  }

  def missedUnauthorised(result: AllAttendanceReportCommandResult): AllAttendanceReportCommandResult = {
    val missedStudents = result.studentDataMap.filter { case (studentData, pointMap) =>
      pointMap.filter { case (point, state) => state == AttendanceState.MissedUnauthorised }.nonEmpty
    }.keySet
    val missedPoints = result.studentDataMap.flatMap { case (studentData, pointMap) =>
      pointMap.filter { case (point, state) => state == AttendanceState.MissedUnauthorised }.keySet
    }.toSeq
    AllAttendanceReportCommandResult(result.studentDataMap.map { case (studentData, pointMap) =>
      studentData -> pointMap.view.filterKeys(missedPoints.contains).toMap
    }.view.filterKeys(missedStudents.contains).toMap
      ,result.reportRangeStartDate, result.reportRangeEndDate)
  }
}
