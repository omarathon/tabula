package uk.ac.warwick.tabula.attendance.commands.profile

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.GroupsPoints
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringNote,AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import scala.collection.immutable.ListMap
// do not remove; import needed for sorting
// should be: import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
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
	checkPointNotes:  Map[String, Seq[(AttendanceMonitoringNote,AttendanceMonitoringCheckpoint)]],
	allNotesWithSomeCheckPoints:  Map[AttendanceMonitoringPoint,(AttendanceMonitoringNote,AttendanceMonitoringCheckpoint)],
	notesWithoutCheckPoints: Seq[(AttendanceMonitoringPoint, AttendanceMonitoringNote)]
	)

class AttendanceProfileCommandInternal(val student: StudentMember, val academicYear: AcademicYear)
	extends CommandInternal[AttendanceProfileCommandResult]
		with GroupsPoints with TaskBenchmarking {

	self: AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def applyInternal() = {
		val points = benchmarkTask("listStudentsPoints") {
			attendanceMonitoringService.listStudentsPoints(student, None, academicYear)
		}
		val checkpointMap = attendanceMonitoringService.getCheckpoints(points, student)
		val groupedPoints = groupByTerm(points, groupSimilar = false) ++ groupByMonth(points, groupSimilar = false)
		//notes corresponding to this student-  this may contain notes that have no checkpoints
		val sortedNotes: Map[AttendanceMonitoringPoint, AttendanceMonitoringNote] = ListMap(attendanceMonitoringService.getAttendanceNoteMap(student).toSeq.sortBy(_._2.updatedDate): _*)
		val notesWithCheckPoints: Map[AttendanceMonitoringPoint, AttendanceMonitoringNote] = sortedNotes.filter { case (amp, amn) => checkpointMap.contains(amp) }
		val notesWithoutCheckPoints: Map[AttendanceMonitoringPoint, AttendanceMonitoringNote] = sortedNotes.filterNot { case (amp, amn) => checkpointMap.contains(amp) }
		val checkPointNotesInfo: Seq[(AttendanceMonitoringNote, AttendanceMonitoringCheckpoint)] = notesWithCheckPoints.map { case (amp, amn) => (amn -> checkpointMap.get(amp).get) }.toSeq
		val checkPointNotesGroupedByStateMap: Map[String, Seq[(AttendanceMonitoringNote, AttendanceMonitoringCheckpoint)]] = checkPointNotesInfo.groupBy { case (amn, amcp) => amcp.state.dbValue }
		val sortedCheckPointNotesGroupedByStateMap: Map[String, Seq[(AttendanceMonitoringNote, AttendanceMonitoringCheckpoint)]] = ListMap(checkPointNotesGroupedByStateMap.toSeq.sortBy(_._1): _*)
		val allSortedNotesWithSomeCheckpoints: Map[AttendanceMonitoringPoint, (AttendanceMonitoringNote, AttendanceMonitoringCheckpoint)] = sortedNotes.map { case (aMonitoringPoint, aMonitoringNote) =>
			aMonitoringPoint ->(aMonitoringNote, checkPointNotesInfo.toMap.getOrElse(aMonitoringNote, null))
		}
		AttendanceProfileCommandResult(
			groupedPoints.map { case (period, thesePoints) =>
				period -> thesePoints.map { groupedPoint =>
					groupedPoint.templatePoint -> checkpointMap.getOrElse(groupedPoint.templatePoint, null)
				}
			},
			sortedCheckPointNotesGroupedByStateMap,
			allSortedNotesWithSomeCheckpoints,
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
