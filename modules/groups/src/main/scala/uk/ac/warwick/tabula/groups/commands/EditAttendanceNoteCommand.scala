package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.data.model.{AbsenceType, FileAttachment, StudentMember}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.CurrentUser
import org.joda.time.DateTime
import org.springframework.validation.{Errors, BindingResult}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEventAttendanceNote, SmallGroupEventAttendance, SmallGroupEventOccurrence}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import java.lang.IllegalArgumentException

object EditAttendanceNoteCommand {
	def apply(student: StudentMember, occurrence: SmallGroupEventOccurrence, user: CurrentUser, customStateStringOption: Option[String]) =
		new EditAttendanceNoteCommand(student, occurrence, user, customStateStringOption)
		with ComposableCommand[SmallGroupEventAttendanceNote]
		with AttendanceNotePermissions
		with AttendanceNoteDescription
		with AttendanceNoteValidation
		with AttendanceNoteCommandState
		with AutowiringFileAttachmentServiceComponent
		with AutowiringProfileServiceComponent
		with AutowiringSmallGroupServiceComponent
}

abstract class EditAttendanceNoteCommand(
	val student: StudentMember,
	val occurrence: SmallGroupEventOccurrence,
	val user: CurrentUser,
	val customStateStringOption: Option[String]
)	extends CommandInternal[SmallGroupEventAttendanceNote] with PopulateOnForm with BindListener with AttendanceNoteCommandState {

	self: SmallGroupServiceComponent with FileAttachmentServiceComponent with ProfileServiceComponent =>

	def populate() = {
		note = attendanceNote.note
		attachedFile = attendanceNote.attachment
		absenceType = attendanceNote.absenceType
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
		customStateStringOption.map(stateString => {
			try {
				customState = AttendanceState.fromCode(stateString)
			} catch {
				case _: IllegalArgumentException =>
			}
		})
	}

	def applyInternal() = {
		attendanceNote.note = note

		if (attendanceNote.attachment != null && attachedFile == null) {
			fileAttachmentService.deleteAttachments(Seq(attendanceNote.attachment))
			attendanceNote.attachment = null
		}

		if (file.hasAttachments) {
			attendanceNote.attachment = file.attached.iterator.next
			attendanceNote.attachment.temporary = false
		}

		attendanceNote.absenceType = absenceType
		attendanceNote.updatedBy = user.apparentId
		attendanceNote.updatedDate = DateTime.now
		smallGroupService.saveOrUpdate(attendanceNote)
		attendanceNote
	}
}

trait AttendanceNoteValidation extends SelfValidating {
	self: AttendanceNoteCommandState =>

	override def validate(errors: Errors) = {
		if (absenceType == null) {
			errors.rejectValue("absenceType", "attendanceNote.absenceType.empty")
		}
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
	var absenceType: AbsenceType = _

	var isNew: Boolean = false
	var attendance: SmallGroupEventAttendance = _
	var attendanceDescription: String = ""
	var customState: AttendanceState = _
}
