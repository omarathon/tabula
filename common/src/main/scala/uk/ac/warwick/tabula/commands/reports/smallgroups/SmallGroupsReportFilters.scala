package uk.ac.warwick.tabula.commands.reports.smallgroups

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.userlookup.User

object SmallGroupsReportFilters {

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

    val unrecordedMap: Map[User, Map[SmallGroupEventWeek, AttendanceState]] = result.attendance.map { case (studentData, eventMap) =>
      studentData -> eventMap.filter { case (event, state) => isUnrecorded(event, state) }
    }.filter { case (_, eventMap) => eventMap.nonEmpty }

    AllSmallGroupsReportCommandResult(
      unrecordedMap,
      result.studentDatas.filter { d => unrecordedMap.keySet.exists(_.getWarwickId == d.universityId) },
      unrecordedMap.flatMap { case (_, attendanceMap) => attendanceMap.keys }.toSeq.distinct.sortBy(sgew => (sgew.week, sgew.event.day.getAsInt))
    )
  }

  def missed(academicYear: AcademicYear)(result: AllSmallGroupsReportCommandResult): AllSmallGroupsReportCommandResult = {
    val missedMap: Map[User, Map[SmallGroupEventWeek, AttendanceState]] = result.attendance.map { case (studentData, eventMap) =>
      studentData -> eventMap.filter { case (_, state) => state == AttendanceState.MissedUnauthorised || state == AttendanceState.MissedAuthorised }
    }.filter { case (_, eventMap) => eventMap.nonEmpty }

    AllSmallGroupsReportCommandResult(
      missedMap,
      result.studentDatas.filter { d => missedMap.keySet.exists(_.getWarwickId == d.universityId) },
      missedMap.flatMap { case (_, attendanceMap) => attendanceMap.keys }.toSeq.distinct.sortBy(sgew => (sgew.week, sgew.event.day.getAsInt))
    )
  }
}
