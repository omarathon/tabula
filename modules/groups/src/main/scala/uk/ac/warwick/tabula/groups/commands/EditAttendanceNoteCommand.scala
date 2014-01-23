package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.data.model.{FileAttachment, StudentMember}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.CurrentUser
import org.joda.time.DateTime
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventAttendanceNote, SmallGroupEventAttendance, SmallGroupEventOccurrence}

object EditAttendanceNoteCommand {
	def apply(student: StudentMember, occurrence: SmallGroupEventOccurrence, user: CurrentUser) =
		new EditAttendanceNoteCommand(student, occurrence, user)
		with ComposableCommand[SmallGroupEventAttendanceNote]
		with AttendanceNotePermissions
		with AttendanceNoteDescription
		with AttendanceNoteCommandState
		with AutowiringFileAttachmentServiceComponent
		with AutowiringProfileServiceComponent
		with AutowiringSmallGroupServiceComponent
}

abstract class EditAttendanceNoteCommand(val student: StudentMember, val occurrence: SmallGroupEventOccurrence, val user: CurrentUser)
	extends CommandInternal[SmallGroupEventAttendanceNote] with PopulateOnForm with BindListener with AttendanceNoteCommandState {

	self: SmallGroupServiceComponent with FileAttachmentServiceComponent with ProfileServiceComponent =>

	def populate() = {
		note = attendanceNote.note
		attachedFile = attendanceNote.attachment
	}

	def onBind(result: BindingResult) {
		file.onBind(result)
		attendanceNote = smallGroupService.getAttendanceNote(student.universityId, occurrence).getOrElse({
			isNew = true
			val newNote = new SmallGroupEventAttendanceNote
			newNote.student = student
			newNote.occurrence = occurrence
			newNote
		})
		attendance = smallGroupService.getAttendance(student.universityId, occurrence).getOrElse(null)
	}

	def applyInternal() = {
		val attendanceNote = smallGroupService.getAttendanceNote(student.universityId, occurrence).getOrElse({
			val newNote = new SmallGroupEventAttendanceNote
			newNote.student = student
			newNote.occurrence = occurrence
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
		smallGroupService.saveOrUpdate(attendanceNote)
		attendanceNote
	}
}

trait AttendanceNoteDescription extends Describable[SmallGroupEventAttendanceNote] {
	self: AttendanceNoteCommandState =>

	override lazy val eventName = "UpdateAttendanceNote"

	override def describe(d: Description) {
		d.studentIds(Seq(student.universityId))
		d.smallGroupEventOccurrence(occurrence)
	}

	override def describeResult(d: Description, result: SmallGroupEventAttendanceNote) {
		d.property("note", result.escapedNote)
	}
}

trait AttendanceNotePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AttendanceNoteCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroupEvents.Register, student)
	}
}

trait AttendanceNoteCommandState {
	def student: StudentMember
	def occurrence: SmallGroupEventOccurrence

	var attendanceNote: SmallGroupEventAttendanceNote = _
	var note: String = _
	var file: UploadedFile = new UploadedFile
	var attachedFile: FileAttachment = _

	var isNew: Boolean = false
	var attendance: SmallGroupEventAttendance = _
	var attendanceDescription: String = ""
}
