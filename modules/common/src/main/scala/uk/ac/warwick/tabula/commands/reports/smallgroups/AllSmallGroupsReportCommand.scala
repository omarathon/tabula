package uk.ac.warwick.tabula.commands.reports.smallgroups

import java.util.UUID

import org.joda.time.DateTime
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent}
import uk.ac.warwick.tabula.commands.reports.{ReportCommandState, ReportPermissions}
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent, AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.userlookup.User

import scala.collection.JavaConverters._

object AllSmallGroupsReportCommand {
	def apply(
		department: Department,
		academicYear: AcademicYear,
		filter: AllSmallGroupsReportCommandResult => AllSmallGroupsReportCommandResult
	) =
		new AllSmallGroupsReportCommandInternal(department, academicYear, filter)
			with AutowiringSmallGroupServiceComponent
			with AutowiringTermServiceComponent
			with ComposableCommand[AllSmallGroupsReportCommandResult]
			with ReportPermissions
			with AllSmallGroupsReportCommandState
			with ReadOnly with Unaudited
}

case class SmallGroupEventWeek(
	id: String, // These are needed so the object can be used as a key in the JSON
	event: SmallGroupEvent,
	week: Int,
	late: Boolean
)

case class AllSmallGroupsReportCommandResult(
	attendance: Map[User, Map[SmallGroupEventWeek, AttendanceState]],
	students: Seq[User],
	eventWeeks: Seq[SmallGroupEventWeek]
)

class AllSmallGroupsReportCommandInternal(
	val department: Department,
	val academicYear: AcademicYear,
	val filter: AllSmallGroupsReportCommandResult => AllSmallGroupsReportCommandResult
) extends CommandInternal[AllSmallGroupsReportCommandResult] with TaskBenchmarking {

	self: SmallGroupServiceComponent with TermServiceComponent =>

	override def applyInternal() = {
		val thisWeek = termService.getAcademicWeekForAcademicYear(DateTime.now, academicYear)
		val thisDay = DateTime.now.getDayOfWeek

		val sets = benchmarkTask("sets") {
			smallGroupService.getAllSmallGroupSets(department).filter(_.academicYear == academicYear).filter(_.collectAttendance)
		}

		val students: Seq[User] = sets.flatMap(_.allStudents).distinct.sortBy(s => (s.getLastName, s.getFirstName))

		val studentInGroup: Map[SmallGroup, Map[User, Boolean]] = benchmarkTask("studentInGroup") {
			sets.flatMap(_.groups.asScala).map(group => group -> students.map(student => student -> group.students.includesUser(student)).toMap).toMap
		}

		// Can't guarantee that all the occurrences will exist for each event,
		// so generate case classes to repesent each occurrence (a combination of event and week)
		val eventWeeks: Seq[SmallGroupEventWeek] = benchmarkTask("eventWeeks") {
			sets.flatMap(_.groups.asScala.flatMap(_.events).filter(!_.isUnscheduled).flatMap(sge => {
				sge.allWeeks.map(week => SmallGroupEventWeek(UUID.randomUUID.toString, sge, week, {
					week < thisWeek || week == thisWeek && sge.day.getAsInt < thisDay
				}))
			})).sortBy(sgew => (sgew.week, sgew.event.day.getAsInt))
		}

		val sgewAttendanceMap = benchmarkTask("attendance") {
			sets.flatMap(_.groups.asScala).flatMap(smallGroupService.findAttendanceByGroup).flatMap(occurrence =>
				// Ignore any occurrences that can't be found in the eventWeeks; probably weeks that have subsequently been removed
				eventWeeks.find(sgew => sgew.event == occurrence.event && sgew.week == occurrence.week).map(sgew => sgew -> occurrence.attendance.asScala)
			).toMap
		}

		val studentAttendanceMap: Map[User, Map[SmallGroupEventWeek, AttendanceState]] = benchmarkTask("studentAttendanceMap") {
			students.map(student => student -> eventWeeks.map(sgew => sgew -> {
				sgewAttendanceMap.get(sgew).flatMap(attendance => {
					// There is some attendance recorded for this SGEW, so see if there is any for this student
					attendance.find(_.universityId == student.getWarwickId).map(_.state)
				}).getOrElse({
					// There is NO attendance recorded for this SGEW
					// No attendance for this student; should there be?
					if (studentInGroup(sgew.event.group)(student)) {
						AttendanceState.NotRecorded
					} else {
						null
					}
				})
			}).filter{case(_, state) => state != null}.toMap).toMap
		}

		filter(AllSmallGroupsReportCommandResult(studentAttendanceMap, students, eventWeeks))
	}
}

trait AllSmallGroupsReportCommandState extends ReportCommandState {
}
