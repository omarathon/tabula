package uk.ac.warwick.tabula.commands.attendance.check

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventAttendance, SmallGroupEventOccurrence}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringEventAttendanceServiceComponent, AutowiringAttendanceMonitoringEventAttendanceServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointGroupProfileServiceComponent, MonitoringPointGroupProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

case class CheckpointResult(
	attendedMonitoringPointStudentList: Seq[StudentMember],
	missedMonitoringPointStudentList: Seq[StudentMember]
)

// TODO: When old-style points are retired have this command return a Seq[AttendanceMonitoringCheckpoint]
object CheckEventAttendanceCheckpointsCommand {
	def apply(occurrence: SmallGroupEventOccurrence) =
		new CheckEventAttendanceCheckpointsCommandInternal(occurrence)
			with ComposableCommand[CheckpointResult]
			with AutowiringMonitoringPointGroupProfileServiceComponent
			with AutowiringAttendanceMonitoringEventAttendanceServiceComponent
			with CheckEventAttendanceCheckpointsPermissions
			with CheckEventAttendanceCheckpointsCommandState
			with ReadOnly with Unaudited
}


class CheckEventAttendanceCheckpointsCommandInternal(val occurrence: SmallGroupEventOccurrence)
	extends CommandInternal[CheckpointResult] {

	self: CheckEventAttendanceCheckpointsCommandState with MonitoringPointGroupProfileServiceComponent
		with AttendanceMonitoringEventAttendanceServiceComponent =>

	override def applyInternal() = {
		val attendanceList = attendances.asScala.map{ case (universityId, state) =>
			val attendance = new SmallGroupEventAttendance
			attendance.occurrence = occurrence
			attendance.state = state
			attendance.universityId = universityId
			attendance
		}.toSeq
		val studentListWithOldCheckpoints = monitoringPointGroupProfileService.getCheckpointsForAttendance(attendanceList).map(a => a.student).distinct

		val studentListWithNewCheckpoints = attendanceMonitoringEventAttendanceService.getCheckpoints(attendanceList).map(a => a.student).distinct
		if (occurrence.event.group.groupSet.module.adminDepartment.autoMarkMissedMonitoringPoints) {
			val studentListWithMissedCheckpoints = attendanceMonitoringEventAttendanceService.getMissedCheckpoints(attendanceList)
				.map { case (a, _) => a.student }.distinct
			CheckpointResult(studentListWithOldCheckpoints ++ studentListWithNewCheckpoints, studentListWithMissedCheckpoints)
		} else {
			CheckpointResult(studentListWithOldCheckpoints ++ studentListWithNewCheckpoints, Seq())
		}
	}

}

trait CheckEventAttendanceCheckpointsPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: CheckEventAttendanceCheckpointsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroupEvents.Register, occurrence)
	}

}

trait CheckEventAttendanceCheckpointsCommandState {
	def occurrence: SmallGroupEventOccurrence
	var attendances: JMap[String, AttendanceState] = JHashMap()
}
