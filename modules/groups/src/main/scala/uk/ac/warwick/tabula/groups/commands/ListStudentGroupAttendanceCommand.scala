package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.commands.{TaskBenchmarking, Unaudited, ComposableCommand, ReadOnly, CommandInternal}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.SmallGroupServiceComponent
import uk.ac.warwick.tabula.services.AutowiringSmallGroupServiceComponent
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.services.TermServiceComponent
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventAttendanceNote, SmallGroupEventOccurrence, SmallGroup, DayOfWeek, WeekRange}
import org.joda.time.DateTime
import scala.collection.immutable.SortedMap
import uk.ac.warwick.util.termdates.Term
import org.joda.time.DateMidnight
import org.joda.time.DateTimeConstants
import uk.ac.warwick.tabula.groups.commands.ViewSmallGroupAttendanceCommand._

case class StudentGroupAttendance(
	termWeeks: SortedMap[Term, WeekRange],
	attendance: ListStudentGroupAttendanceCommand.PerTermAttendance,
	notes: Map[EventInstance, SmallGroupEventAttendanceNote],
	missedCount: Int,
	missedCountByTerm: Map[Term, Int]
)

object ListStudentGroupAttendanceCommand {
	import ViewSmallGroupAttendanceCommand._

	type PerGroupAttendance = SortedMap[SmallGroup, SortedMap[SmallGroupEventOccurrence.WeekNumber, SortedMap[EventInstance, SmallGroupAttendanceState]]]
	type PerTermAttendance = SortedMap[Term, PerGroupAttendance]

	def apply(member: Member, academicYear: AcademicYear) =
		new ListStudentGroupAttendanceCommandInternal(member, academicYear)
			with ComposableCommand[StudentGroupAttendance]
			with ListStudentGroupAttendanceCommandPermissions
			with AutowiringSmallGroupServiceComponent
			with AutowiringTermServiceComponent
			with ReadOnly with Unaudited
}

class ListStudentGroupAttendanceCommandInternal(val member: Member, val academicYear: AcademicYear)
	extends CommandInternal[StudentGroupAttendance]
		with ListStudentGroupAttendanceCommandState with TaskBenchmarking {
	self: SmallGroupServiceComponent with TermServiceComponent =>

	import ViewSmallGroupAttendanceCommand._

	implicit val defaultOrderingForGroup = Ordering.by { group: SmallGroup => (group.groupSet.module.code, group.groupSet.name, group.name, group.id) }
	implicit val defaultOrderingForDateTime = Ordering.by[DateTime, Long] ( _.getMillis )
	implicit val defaultOrderingForTerm = Ordering.by[Term, DateTime] ( _.getStartDate )

	def applyInternal() = {
		val user = member.asSsoUser

		val groups = smallGroupService.findSmallGroupsByStudent(user).filter {
			group =>
				group.groupSet.showAttendanceReports &&
				group.groupSet.visibleToStudents &&
				group.groupSet.academicYear == academicYear &&
				!group.events.asScala.isEmpty
		}

		val allInstances = groups.flatMap { group => allEventInstances(group, smallGroupService.findAttendanceByGroup(group)) }

		val attendance = groupByTerm(allInstances).mapValues { instances =>
			val groups = SortedMap(instances.groupBy { case ((event, _), _) => event.group }.toSeq:_*)
			groups.mapValues { instances =>
				SortedMap(instances.groupBy { case ((_, week), _) => week }.toSeq:_*).mapValues { instances =>
					attendanceForStudent(instances, isLate)(user)
				}
			}
		}

		val missedCountByTerm = attendance.mapValues { groups =>
			val count = groups.map { case (_, attendanceByInstance) =>
				attendanceByInstance.values.flatMap(_.values).count(_ == SmallGroupAttendanceState.MissedUnauthorised)
			}

			count.foldLeft(0) { (acc, missedCount) => acc + missedCount }
		}

		val termWeeks = SortedMap(attendance.keySet.map { term =>
			term -> WeekRange(
				termService.getAcademicWeekForAcademicYear(term.getStartDate, academicYear),
				termService.getAcademicWeekForAcademicYear(term.getEndDate, academicYear)
			)
		}.toSeq:_*)

		val attendanceNotes = benchmarkTask("Get attendance notes") {
			smallGroupService.findAttendanceNotes(
				Seq(user.getWarwickId),
				allInstances.flatMap{case(_, occurenceOption) => occurenceOption}
			).groupBy(n => (n.occurrence.event, n.occurrence.week)).mapValues(_.head)
		}

		StudentGroupAttendance(
			termWeeks,
			attendance,
			attendanceNotes,
			missedCountByTerm.foldLeft(0) { (acc, missedByTerm) => acc + missedByTerm._2 },
			missedCountByTerm
		)
	}

	def groupByTerm(instances: Seq[(EventInstance, Option[SmallGroupEventOccurrence])]): SortedMap[Term, Seq[(EventInstance, Option[SmallGroupEventOccurrence])]] = {
		val approxStartDate = new DateMidnight(academicYear.startYear, DateTimeConstants.NOVEMBER, 1)
		val day = DayOfWeek.Thursday
		lazy val weeksForYear = termService.getAcademicWeeksForYear(approxStartDate).toMap

		SortedMap(instances.groupBy { case ((_, week), _) =>
			val date = weeksForYear(week).getStart.withDayOfWeek(day.jodaDayOfWeek)
			termService.getTermFromDateIncludingVacations(date)
		}.toSeq:_*)
	}

	lazy val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(DateTime.now, academicYear)

	private def isLate(instance: EventInstance): Boolean = instance match {
		case (_, week: SmallGroupEventOccurrence.WeekNumber) =>
			week < currentAcademicWeek // only late if week is in the past
	}
}

trait ListStudentGroupAttendanceCommandState {
	def member: Member
	def academicYear: AcademicYear
}

trait ListStudentGroupAttendanceCommandPermissions extends RequiresPermissionsChecking {
	self: ListStudentGroupAttendanceCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.Read.SmallGroups, member)
		p.PermissionCheck(Permissions.SmallGroupEvents.ViewRegister, member)
	}
}