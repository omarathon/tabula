package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence

object AttendanceNoteAttachmentCommand {
	def apply(student: StudentMember, occurrence: SmallGroupEventOccurrence, user: CurrentUser) =
		new AttendanceNoteAttachmentCommand(student, occurrence, user)
		with ComposableCommand[Option[RenderableFile]]
		with ApplyWithCallback[Option[RenderableFile]]
		with AutowiringSmallGroupServiceComponent
		with ReadOnly
		with AttendanceNoteAttachmentPermissions
		with AttendanceNoteCommandState
		with AttendanceNoteAttachmentDescription
}

class AttendanceNoteAttachmentCommand(val student: StudentMember, val occurrence: SmallGroupEventOccurrence, val user: CurrentUser)
	extends CommandInternal[Option[RenderableFile]] with HasCallback[Option[RenderableFile]] {

	self: SmallGroupServiceComponent =>

	def applyInternal() = {
		val result = smallGroupService.getAttendanceNote(student.universityId, occurrence).flatMap{ note =>
			Option(note.attachment).map{ attachment =>
				new RenderableAttachment(attachment)
			}
		}
		callback(result)
		result
	}

}

trait AttendanceNoteAttachmentPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: AttendanceNoteCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroupEvents.ViewRegister, student)
	}
}

trait AttendanceNoteAttachmentDescription extends Describable[Option[RenderableFile]] {
	self: AttendanceNoteCommandState =>

	override lazy val eventName = "DownloadAttendanceNoteAttachment"

	override def describe(d: Description) {
		d.studentIds(Seq(student.universityId))
		d.smallGroupEventOccurrence(occurrence)
	}
}