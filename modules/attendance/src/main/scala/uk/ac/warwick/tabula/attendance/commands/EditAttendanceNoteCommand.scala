package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.{FileAttachment, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpoint, MonitoringPointAttendanceNote, MonitoringPoint}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent, FileAttachmentServiceComponent, AutowiringFileAttachmentServiceComponent, AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent}
import uk.ac.warwick.tabula.CurrentUser
import org.joda.time.DateTime
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.system.BindListener

object EditAttendanceNoteCommand {
	def apply(student: StudentMember, monitoringPoint: MonitoringPoint, user: CurrentUser) =
		new EditAttendanceNoteCommand(student, monitoringPoint, user)
		with ComposableCommand[MonitoringPointAttendanceNote]
		with AttendanceNotePermissions
		with AttendanceNoteDescription
		with AttendanceNoteCommandState
		with AutowiringMonitoringPointServiceComponent
		with AutowiringFileAttachmentServiceComponent
		with AutowiringProfileServiceComponent
}

abstract class EditAttendanceNoteCommand(val student: StudentMember, val monitoringPoint: MonitoringPoint, val user: CurrentUser)
	extends CommandInternal[MonitoringPointAttendanceNote] with PopulateOnForm with BindListener with AttendanceNoteCommandState with CheckpointUpdatedDescription {

	self: MonitoringPointServiceComponent with FileAttachmentServiceComponent with ProfileServiceComponent =>

	def populate() = {
		note = attendanceNote.note
		attachedFile = attendanceNote.attachment
	}

	def onBind(result: BindingResult) {
		file.onBind(result)
		attendanceNote = monitoringPointService.getAttendanceNote(student, monitoringPoint).getOrElse({
			isNew = true
			val newNote = new MonitoringPointAttendanceNote
			newNote.student = student
			newNote.point = monitoringPoint
			newNote
		})
		checkpoint = monitoringPointService.getCheckpoint(student, monitoringPoint).getOrElse(null)
		checkpointDescription = Option(checkpoint).map{ checkpoint => describeCheckpoint(checkpoint)}.getOrElse("")
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

	var isNew: Boolean = false
	var checkpoint: MonitoringCheckpoint = _
	var checkpointDescription: String = ""
}
