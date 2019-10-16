package uk.ac.warwick.tabula.commands.reports.smallgroups

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringService

object SmallGroupsReportFilters {

  val attendanceMonitoringService: AttendanceMonitoringService = Wire[AttendanceMonitoringService]

  def identity(result: AllSmallGroupsReportCommandResult): AllSmallGroupsReportCommandResult = result

  def unrecorded(academicYear: AcademicYear)(result: AllSmallGroupsReportCommandResult): AllSmallGroupsReportCommandResult = {
    lazy val thisWeek = academicYear.weekForDate(LocalDate.now).weekNumber
    lazy val thisDay = DateTime.now.getDayOfWeek

    def isUnrecorded(event: SmallGroupEventWeek, state: AttendanceState): Boolean = {
      state == AttendanceState.NotRecorded &&
      (
        academicYear < AcademicYear.now() ||
        (academicYear == AcademicYear.now() && (
          event.week < thisWeek ||
          (event.week == thisWeek && event.event.day.getAsInt < thisDay)
        ))
      )
    }

    val unrecordedMap = result.attendance.map { case (studentData, eventMap) =>
      studentData -> eventMap.filter { case (event, state) => isUnrecorded(event, state) }
    }.filter { case (_, eventMap) => eventMap.nonEmpty }
    AllSmallGroupsReportCommandResult(
      unrecordedMap,
      attendanceMonitoringService.getAttendanceMonitoringDataForStudents(unrecordedMap.keySet.toSeq.sortBy(s => (s.getLastName, s.getFirstName)).map(_.getWarwickId), academicYear),
      unrecordedMap.flatMap { case (_, attendanceMap) => attendanceMap.keys }.toSeq.distinct.sortBy(sgew => (sgew.week, sgew.event.day.getAsInt))
    )
  }

  def missed(academicYear: AcademicYear)(result: AllSmallGroupsReportCommandResult): AllSmallGroupsReportCommandResult = {
    val missedMap = result.attendance.map { case (studentData, eventMap) =>
      studentData -> eventMap.filter { case (_, state) => state == AttendanceState.MissedUnauthorised || state == AttendanceState.MissedAuthorised }
    }.filter { case (studentData, eventMap) => eventMap.nonEmpty }
    AllSmallGroupsReportCommandResult(
      missedMap,
      attendanceMonitoringService.getAttendanceMonitoringDataForStudents(missedMap.keySet.toSeq.sortBy(s => (s.getLastName, s.getFirstName)).map(_.getWarwickId), academicYear),
      missedMap.flatMap { case (_, attendanceMap) => attendanceMap.keys }.toSeq.distinct.sortBy(sgew => (sgew.week, sgew.event.day.getAsInt))
    )
  }
}
