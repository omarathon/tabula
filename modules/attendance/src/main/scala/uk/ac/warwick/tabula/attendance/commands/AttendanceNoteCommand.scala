package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.{FileAttachment, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointAttendanceNote, MonitoringPoint}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{FileAttachmentServiceComponent, AutowiringFileAttachmentServiceComponent, AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent}
import uk.ac.warwick.tabula.CurrentUser
import org.joda.time.DateTime
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.system.BindListener

object AttendanceNoteCommand {
	def apply(student: StudentMember, monitoringPoint: MonitoringPoint, user: CurrentUser) =
		new AttendanceNoteCommand(student, monitoringPoint, user)
		with ComposableCommand[MonitoringPointAttendanceNote]
		with AttendanceNotePermissions
		with AttendanceNoteDescription
		with AttendanceNoteCommandState
		with AutowiringMonitoringPointServiceComponent
		with AutowiringFileAttachmentServiceComponent
}

class AttendanceNoteCommand(val student: StudentMember, val monitoringPoint: MonitoringPoint, val user: CurrentUser)
	extends CommandInternal[MonitoringPointAttendanceNote] with PopulateOnForm with BindListener with AttendanceNoteCommandState {

	self: MonitoringPointServiceComponent with FileAttachmentServiceComponent =>

	def populate() = {
		attendanceNote = monitoringPointService.getAttendanceNote(student, monitoringPoint).getOrElse({
			val newNote = new MonitoringPointAttendanceNote
			newNote.student = student
			newNote.point = monitoringPoint
			newNote
		})
		note = attendanceNote.note
		attachedFile = attendanceNote.attachment
	}

	def onBind(result: BindingResult) {
		file.onBind(result)
	}

	def applyInternal() = {
		val attendanceNote = monitoringPointService.getAttendanceNote(student, monitoringPoint).getOrElse({
			val newNote = new MonitoringPointAttendanceNote
			newNote.student = student
			newNote.point = monitoringPoint
			newNote
		})
		attendanceNote.note = note

		if (attendanceNote.attachment != null && attachedFile == null) {
			fileAttachmentService.deleteAttachments(Seq(attendanceNote.attachment))
			attendanceNote.attachment = null
		}

		if (file.hasAttachments) {
			attendanceNote.attachment = file.attached.iterator.next
			attendanceNote.attachment.temporary = false
		}

		attendanceNote.updatedBy = user.apparentId
		attendanceNote.updatedDate = DateTime.now
		monitoringPointService.saveOrUpdate(attendanceNote)
		attendanceNote
	}
}

trait AttendanceNoteDescription extends Describable[MonitoringPointAttendanceNote] {
	self: AttendanceNoteCommandState =>

	override lazy val eventName = "UpdateAttendanceNote"

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
		p.PermissionCheck(Permissions.MonitoringPoints.Record, student)
	}
}

trait AttendanceNoteCommandState {
	def student: StudentMember
	def monitoringPoint: MonitoringPoint

	var attendanceNote: MonitoringPointAttendanceNote = _
	var note: String = _
	var file: UploadedFile = new UploadedFile
	var attachedFile: FileAttachment = _
}
