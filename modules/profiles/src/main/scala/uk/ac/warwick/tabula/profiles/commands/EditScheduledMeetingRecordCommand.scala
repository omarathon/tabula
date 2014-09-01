package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.{BindingResult, Errors}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.services.{MeetingRecordServiceComponent, AutowiringMeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, FileAttachmentServiceComponent}
import uk.ac.warwick.tabula.data.model.notifications.ScheduledMeetingRecordInviteeNotification

case class ScheduledMeetingRecordResult(meetingRecord: ScheduledMeetingRecord, isRescheduled: Boolean) extends ToEntityReference {
	override type Entity = AbstractMeetingRecord
	override def toEntityReference = meetingRecord.toEntityReference
}

object EditScheduledMeetingRecordCommand {
	def apply(creator: Member, meetingRecord: ScheduledMeetingRecord) =
		new EditScheduledMeetingRecordCommand(creator, meetingRecord)
			with ComposableCommand[ScheduledMeetingRecordResult]
			with EditScheduledMeetingRecordPermissions
			with EditScheduledMeetingRecordState
			with EditScheduledMeetingRecordDescription
			with AutowiringMeetingRecordServiceComponent
			with EditScheduledMeetingRecordCommandValidation
			with EditScheduledMeetingRecordNotification
			with AutowiringFileAttachmentServiceComponent
			with EditScheduledMeetingRecordCommandPopulate
			with ModifyScheduledMeetingRecordNotifications
}

trait EditScheduledMeetingRecordCommandPopulate	extends PopulateOnForm {
	self: EditScheduledMeetingRecordState =>

	override def populate() = {
		title = meetingRecord.title
		description = meetingRecord.description
		meetingDate = meetingRecord.meetingDate
		format = meetingRecord.format
		attachedFiles = meetingRecord.attachments
	}
}

class EditScheduledMeetingRecordCommand (val creator: Member, val meetingRecord: ScheduledMeetingRecord)
	extends CommandInternal[ScheduledMeetingRecordResult] with EditScheduledMeetingRecordState with BindListener {

	self: MeetingRecordServiceComponent with FileAttachmentServiceComponent =>

	def applyInternal() = {

		def persistAttachments(meeting: ScheduledMeetingRecord) {
			// delete attachments that have been removed

			if (meeting.attachments != null) {
				val filesToKeep = Option(attachedFiles).map(_.asScala.toList).getOrElse(List())
				val filesToRemove = meeting.attachments.asScala -- filesToKeep
				meeting.attachments = JArrayList[FileAttachment](filesToKeep)
				fileAttachmentService.deleteAttachments(filesToRemove)
			}

			file.attached.asScala map(attachment => {
				attachment.meetingRecord = meeting
				meeting.attachments.add(attachment)
				attachment.temporary = false
			})
		}

		meetingRecord.title = title
		meetingRecord.description = description
		val isRescheduled = meetingRecord.meetingDate != meetingDate
		meetingRecord.meetingDate = meetingDate
		meetingRecord.lastUpdatedDate = DateTime.now
		meetingRecord.format = format

		persistAttachments(meetingRecord)
		meetingRecordService.saveOrUpdate(meetingRecord)
		ScheduledMeetingRecordResult(meetingRecord, isRescheduled)
	}

	def onBind(result: BindingResult) {
		file.onBind(result)
	}

}

trait EditScheduledMeetingRecordCommandValidation extends SelfValidating with ScheduledMeetingRecordValidation {
	self: EditScheduledMeetingRecordState with MeetingRecordServiceComponent =>
	override def validate(errors: Errors) {
		sharedValidation(errors, title, meetingDate)
		meetingRecordService.listScheduled(Set(meetingRecord.relationship), Some(creator)).foreach(
			m => if (m.meetingDate == meetingDate && m.id != meetingRecord.id) errors.rejectValue("meetingDate", "meetingRecord.date.duplicate")
		)
	}
}

trait EditScheduledMeetingRecordState {
	def creator: Member
	def meetingRecord: ScheduledMeetingRecord

	var title: String = _
	var description: String = _
	var meetingDate: DateTime = _
	var format: MeetingFormat = _

	var file: UploadedFile = new UploadedFile
	var attachedFiles:JList[FileAttachment] = _

	var attachmentTypes = Seq[String]()

	lazy val relationship: StudentRelationship = meetingRecord.relationship
}

trait EditScheduledMeetingRecordPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditScheduledMeetingRecordState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.ScheduledMeetingRecord.Update, mandatory(meetingRecord))
	}
}

trait EditScheduledMeetingRecordDescription extends Describable[ScheduledMeetingRecordResult] {
	self: EditScheduledMeetingRecordState =>

	override lazy val eventName = "EditScheduledMeetingRecord"

	override def describe(d: Description) {
		meetingRecord.relationship.studentMember.map { d.member }
		d.properties(
			"creator" -> creator.universityId,
			"relationship" -> meetingRecord.relationship.relationshipType.toString()
		)
	}
}

trait EditScheduledMeetingRecordNotification extends Notifies[ScheduledMeetingRecordResult, ScheduledMeetingRecord] {
	def emit(result: ScheduledMeetingRecordResult) = {
		val meeting = result.meetingRecord
		val user = meeting.creator.asSsoUser
		val verb =
			if (result.isRescheduled) "rescheduled"
			else "updated"

		Seq(Notification.init(new ScheduledMeetingRecordInviteeNotification(verb), user, meeting, meeting.relationship))
	}
}
