package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions

object AttendanceNoteAttachmentCommand {
	def apply(student: StudentMember, monitoringPoint: MonitoringPoint, user: CurrentUser) =
		new AttendanceNoteAttachmentCommand(student, monitoringPoint, user)
		with ComposableCommand[Option[RenderableFile]]
		with ApplyWithCallback[Option[RenderableFile]]
		with AutowiringMonitoringPointServiceComponent
		with ReadOnly
		with AttendanceNoteAttachmentPermissions
		with AttendanceNoteCommandState
		with AttendanceNoteAttachmentDescription
}

class AttendanceNoteAttachmentCommand(val student: StudentMember, val monitoringPoint: MonitoringPoint, val user: CurrentUser)
	extends CommandInternal[Option[RenderableFile]] with HasCallback[Option[RenderableFile]] {

	self: MonitoringPointServiceComponent =>

	def applyInternal() = {
		val result = monitoringPointService.getAttendanceNote(student, monitoringPoint).flatMap{ note =>
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
		p.PermissionCheck(Permissions.MonitoringPoints.View, student)
	}
}

trait AttendanceNoteAttachmentDescription extends Describable[Option[RenderableFile]] {
	self: AttendanceNoteCommandState =>

	override lazy val eventName = "DownloadAttendanceNoteAttachment"

	override def describe(d: Description) {
		d.studentIds(Seq(student.universityId))
		d.monitoringPoint(monitoringPoint)
	}
}