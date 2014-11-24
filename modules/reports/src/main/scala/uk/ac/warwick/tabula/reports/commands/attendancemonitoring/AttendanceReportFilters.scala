package uk.ac.warwick.tabula.reports.commands.attendancemonitoring

import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.reports.commands.attendancemonitoring.AllAttendanceReportCommand._

object AttendanceReportFilters {

	def identity(result: AllAttendanceReportCommandResult): AllAttendanceReportCommandResult = result

	def unrecorded(result: AllAttendanceReportCommandResult): AllAttendanceReportCommandResult = {
		val unrecordedPoints = result.flatMap { case (studentData, pointMap) =>
			pointMap.filter{ case (point, _) => point.endDate.toDateTimeAtStartOfDay.isBeforeNow }.keySet
		}.toSeq
		result.map{case(studentData, pointMap) =>
			studentData -> pointMap.filterKeys(unrecordedPoints.contains)
		}.filter{case(_, pointMap) => pointMap.nonEmpty}
	}

	def missed(result: AllAttendanceReportCommandResult): AllAttendanceReportCommandResult = {
		val missedStudents = result.filter{case(studentData, pointMap) =>
			pointMap.filter{case(point, state) => state == AttendanceState.MissedUnauthorised}.nonEmpty
		}.keySet
		val missedPoints = result.flatMap { case (studentData, pointMap) =>
			pointMap.filter{ case (point, state) => state == AttendanceState.MissedUnauthorised }.keySet
		}.toSeq
		result.map{case(studentData, pointMap) =>
			studentData -> pointMap.filterKeys(missedPoints.contains)
		}.filterKeys(missedStudents.contains)
	}
}
