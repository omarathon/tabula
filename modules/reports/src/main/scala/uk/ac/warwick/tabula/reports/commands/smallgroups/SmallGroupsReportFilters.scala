package uk.ac.warwick.tabula.reports.commands.smallgroups

import org.joda.time.DateTime
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.services.TermService

object SmallGroupsReportFilters {

	val termService = Wire[TermService]

	def identity(result: AllSmallGroupsReportCommandResult): AllSmallGroupsReportCommandResult = result

	def unrecorded(academicYear: AcademicYear)(result: AllSmallGroupsReportCommandResult): AllSmallGroupsReportCommandResult = {
		val thisWeek = termService.getAcademicWeekForAcademicYear(DateTime.now, academicYear)
		val thisDay = DateTime.now.getDayOfWeek
		val unrecordedMap = result.attendance.map{ case(studentData, eventMap) =>
			studentData -> eventMap.filter { case (event, state) =>
				(event.week < thisWeek || event.week == thisWeek && event.event.day.getAsInt < thisDay) &&
					state == AttendanceState.NotRecorded
			}
		}.filter{ case(studentData, eventMap) => eventMap.nonEmpty }
		AllSmallGroupsReportCommandResult(
			unrecordedMap,
			unrecordedMap.keySet.toSeq.sortBy(s => (s.getLastName, s.getFirstName)),
			unrecordedMap.flatMap(_._2.map(_._1)).toSeq.distinct.sortBy(sgew => (sgew.week, sgew.event.day.getAsInt))
		)
	}

	def missed(result: AllSmallGroupsReportCommandResult): AllSmallGroupsReportCommandResult = {
		val missedMap = result.attendance.map{ case(studentData, eventMap) =>
			studentData -> eventMap.filter { case (_, state) =>	state == AttendanceState.MissedUnauthorised	}
		}.filter{ case(studentData, eventMap) => eventMap.nonEmpty }
		AllSmallGroupsReportCommandResult(
			missedMap,
			missedMap.keySet.toSeq.sortBy(s => (s.getLastName, s.getFirstName)),
			missedMap.flatMap(_._2.map(_._1)).toSeq.distinct.sortBy(sgew => (sgew.week, sgew.event.day.getAsInt))
		)
	}
}
