package uk.ac.warwick.tabula.commands.profiles.attendance

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.GroupsPoints
import uk.ac.warwick.tabula.commands.groups.ListStudentGroupAttendanceCommand
import uk.ac.warwick.tabula.commands.groups.ViewSmallGroupAttendanceCommand._
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, TaskBenchmarking, Unaudited}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringNote, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventAttendanceNote, WeekRange}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringServiceComponent, AutowiringAttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.util.termdates.Term

import scala.collection.immutable.{ListMap, SortedMap}

object MonitoringPointAttendanceProfileCommand {
	def apply(student: StudentMember, academicYear: AcademicYear) =
		new MonitoringPointAttendanceProfileCommandInternal(student, academicYear)
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with ComposableCommand[MonitoringPointAttendanceProfileCommandResult]
			with AttendanceProfilePermissions
			with AttendanceProfileCommandState
			with ReadOnly with Unaudited
}

case class StudentGroupAttendance(
	termWeeks: SortedMap[Term, WeekRange],
	attendance: ListStudentGroupAttendanceCommand.PerTermAttendance,
	notes: Map[EventInstance, SmallGroupEventAttendanceNote],
	missedCount: Int,
	missedCountByTerm: Map[Term, Int]
)

case class MonitoringPointAttendanceProfileCommandResult(
	monitoringPointAttendanceWithCheckPoint: Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]],
	checkPointNotes: Map[String, Seq[(AttendanceMonitoringNote,AttendanceMonitoringCheckpoint)]],
	allNotesWithSomeCheckPoints: Map[AttendanceMonitoringPoint,(AttendanceMonitoringNote,AttendanceMonitoringCheckpoint)],
	notesWithoutCheckPoints: Seq[(AttendanceMonitoringPoint, AttendanceMonitoringNote)]
)

class MonitoringPointAttendanceProfileCommandInternal(val student: StudentMember, val academicYear: AcademicYear)
	extends CommandInternal[MonitoringPointAttendanceProfileCommandResult]
		with GroupsPoints with TaskBenchmarking {

	self: AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def applyInternal() = {
		val points = benchmarkTask("listStudentsPoints") {
			attendanceMonitoringService.listStudentsPoints(student, None, academicYear)
		}
		val checkpointMap = attendanceMonitoringService.getCheckpoints(points, student)
		val groupedPoints = groupByTerm(points, groupSimilar = false) ++ groupByMonth(points, groupSimilar = false)
		//notes corresponding to this student-  this may contain notes that have no checkpoints
		val sortedNotes = ListMap(attendanceMonitoringService.getAttendanceNoteMap(student).toSeq.sortBy { case (_, note) => note.updatedDate }: _*)
		val notesWithCheckPoints = sortedNotes.filter { case (point, _) => checkpointMap.contains(point) }
		val notesWithoutCheckPoints = sortedNotes.filterNot { case (point, _) => checkpointMap.contains(point) }
		val notesWithcheckPointInfo = notesWithCheckPoints.map { case (point, note) => (note -> checkpointMap.get(point).get) }.toSeq
		val notesWithcheckPointInfoGroupedByStateMap = notesWithcheckPointInfo.groupBy { case (_, checkpoint) => checkpoint.state.dbValue }
		val sortedNotesWithcheckPointInfoGroupedByStateMap  = ListMap(notesWithcheckPointInfoGroupedByStateMap.toSeq.sortBy { case (state, _) => state }: _*)
		val allSortedNotesWithSomeCheckpointInfo = sortedNotes.map { case (point, note) =>
			point ->(note, notesWithcheckPointInfo.toMap.getOrElse(note, null))
		}
		MonitoringPointAttendanceProfileCommandResult(
			groupedPoints.map { case (period, thesePoints) =>
				period -> thesePoints.map { groupedPoint =>
					groupedPoint.templatePoint -> checkpointMap.getOrElse(groupedPoint.templatePoint, null)
				}
			},
			sortedNotesWithcheckPointInfoGroupedByStateMap,
			allSortedNotesWithSomeCheckpointInfo,
			notesWithoutCheckPoints.toSeq
		)
	}
}

trait AttendanceProfilePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AttendanceProfileCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, student)
	}

}

trait AttendanceProfileCommandState {
	def academicYear: AcademicYear
	def student: StudentMember
}
