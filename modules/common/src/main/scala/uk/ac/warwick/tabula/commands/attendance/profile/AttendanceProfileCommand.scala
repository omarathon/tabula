package uk.ac.warwick.tabula.commands.attendance.profile

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.attendance.GroupsPoints
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceState, AttendanceMonitoringCheckpoint, AttendanceMonitoringNote, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import scala.collection.immutable.ListMap
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._

object AttendanceProfileCommand {
	def apply(student: StudentMember, academicYear: AcademicYear) =
		new AttendanceProfileCommandInternal(student, academicYear)
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with ComposableCommand[AttendanceProfileCommandResult]
			with AttendanceProfilePermissions
			with AttendanceProfileCommandState
			with ReadOnly with Unaudited
}

case class AttendanceProfileCommandResult(
	attendanceMonitoringPointWithCheckPoint: Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]],
	checkPointNotes: Map[String, Seq[(AttendanceMonitoringNote,AttendanceMonitoringCheckpoint)]],
	allNotesWithSomeCheckPoints: Map[AttendanceMonitoringPoint,(AttendanceMonitoringNote,AttendanceMonitoringCheckpoint)],
	notesWithoutCheckPoints: Seq[(AttendanceMonitoringPoint, AttendanceMonitoringNote)],
	missedPointCountByTerm: Map[String, Int],
	hasAnyMissedPoints: Boolean
	)

class AttendanceProfileCommandInternal(val student: StudentMember, val academicYear: AcademicYear)
	extends CommandInternal[AttendanceProfileCommandResult]
		with GroupsPoints with TaskBenchmarking {

	self: AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def applyInternal(): AttendanceProfileCommandResult = {
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

		val attendanceMonitoringPointWithCheckPoint = groupedPoints.map { case (period, thesePoints) =>
			period -> thesePoints.map { groupedPoint =>
				groupedPoint.templatePoint -> checkpointMap.getOrElse(groupedPoint.templatePoint, null)
			}
		}

		val missedPointCountByTerm = attendanceMonitoringPointWithCheckPoint.map { case (period, pointCheckpointPairs) =>
			period -> pointCheckpointPairs.count { case (point, checkpoint) => checkpoint != null && checkpoint.state == AttendanceState.MissedUnauthorised }
		}

		val hasAnyMissedPoints = missedPointCountByTerm.exists { case(_, pointCount) => pointCount > 0 }

		AttendanceProfileCommandResult(
			attendanceMonitoringPointWithCheckPoint,
			sortedNotesWithcheckPointInfoGroupedByStateMap,
			allSortedNotesWithSomeCheckpointInfo,
			notesWithoutCheckPoints.toSeq,
			missedPointCountByTerm,
			hasAnyMissedPoints
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
