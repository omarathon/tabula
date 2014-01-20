package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointAttendanceNote, MonitoringPoint}
import uk.ac.warwick.tabula.commands.{PopulateOnForm, Description, Describable, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent}
import uk.ac.warwick.tabula.CurrentUser
import org.joda.time.DateTime

object AttendanceNoteCommand {
	def apply(student: StudentMember, monitoringPoint: MonitoringPoint, user: CurrentUser) =
		new AttendanceNoteCommand(student, monitoringPoint, user)
		with ComposableCommand[MonitoringPointAttendanceNote]
		with AttendanceNotePermissions
		with AttendanceNoteDescription
		with AttendanceNoteCommandState
		with AutowiringMonitoringPointServiceComponent
}

class AttendanceNoteCommand(val student: StudentMember, val monitoringPoint: MonitoringPoint, val user: CurrentUser)
	extends CommandInternal[MonitoringPointAttendanceNote] with PopulateOnForm with AttendanceNoteCommandState {

	self: MonitoringPointServiceComponent =>

	def populate() = {
		attendanceNote = monitoringPointService.getAttendanceNote(student, monitoringPoint).getOrElse({
			val newNote = new MonitoringPointAttendanceNote
			newNote.student = student
			newNote.point = monitoringPoint
			newNote
		})
		note = attendanceNote.note
	}

	def applyInternal() = {
		val attendanceNote = monitoringPointService.getAttendanceNote(student, monitoringPoint).getOrElse({
			val newNote = new MonitoringPointAttendanceNote
			newNote.student = student
			newNote.point = monitoringPoint
			newNote
		})
		attendanceNote.note = note
		attendanceNote.updatedBy = user.apparentId
		attendanceNote.updatedDate = DateTime.now
		monitoringPointService.saveOrUpdate(attendanceNote)
		attendanceNote
	}
}

trait AttendanceNoteDescription extends Describable[MonitoringPointAttendanceNote] {
	self: AttendanceNoteCommandState =>

	override lazy val eventName = "AgentStudentRecordCheckpoints"

	override def describe(d: Description) {
		d.studentIds(Seq(student.universityId))
		d.monitoringPoint(monitoringPoint)
	}

	override def describeResult(d: Description, result: MonitoringPointAttendanceNote) {
		d.property("note", result.escapedNote)
	}
}

trait AttendanceNotePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AttendanceNoteCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.View, student)
	}
}

trait AttendanceNoteCommandState {
	def student: StudentMember
	def monitoringPoint: MonitoringPoint

	var attendanceNote: MonitoringPointAttendanceNote = _
	var note: String = _
}
