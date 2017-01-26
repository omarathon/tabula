package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import uk.ac.warwick.tabula.DateFormats.DateTimePickerFormatter
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.{AddsIcalAttachmentToScheduledMeetingNotification, ScheduledMeetingRecordBehalfNotification, ScheduledMeetingRecordInviteeNotification, ScheduledMeetingRecordNotification}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringMeetingRecordServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object CreateScheduledMeetingRecordCommand {
	def apply(creator: Member, relationship: StudentRelationship, considerAlternatives: Boolean) =
		new CreateScheduledMeetingRecordCommand(creator, relationship, considerAlternatives)
			with ComposableCommand[ScheduledMeetingRecord]
			with CreateScheduledMeetingPermissions
			with CreateScheduledMeetingRecordState
			with CreateScheduledMeetingRecordDescription
			with AutowiringMeetingRecordServiceComponent
			with CreateScheduledMeetingRecordCommandValidation
			with CreateScheduledMeetingRecordNotification
			with CreateScheduledMeetingRecordScheduledNotifications
			with PopulateOnForm {
			override def populate(): Unit = {}
		}
}

class CreateScheduledMeetingRecordCommand (val creator: Member, val relationship: StudentRelationship, val considerAlternatives: Boolean = false)
	extends CommandInternal[ScheduledMeetingRecord] with CreateScheduledMeetingRecordState with BindListener {

	self: MeetingRecordServiceComponent =>

	def applyInternal(): ScheduledMeetingRecord = {
		val scheduledMeeting = new ScheduledMeetingRecord(creator, relationship)
		scheduledMeeting.title = title
		scheduledMeeting.description = description

		scheduledMeeting.meetingDate = DateTimePickerFormatter.parseDateTime(meetingDateStr+" "+meetingTimeStr).withHourOfDay(DateTimePickerFormatter.parseDateTime(meetingDateStr+" "+meetingTimeStr).getHourOfDay)
		scheduledMeeting.meetingEndDate = DateTimePickerFormatter.parseDateTime(meetingDateStr+" "+meetingEndTimeStr).withHourOfDay(DateTimePickerFormatter.parseDateTime(meetingDateStr+" "+meetingEndTimeStr).getHourOfDay)
		scheduledMeeting.meetingLocation = meetingLocation

		scheduledMeeting.lastUpdatedDate = DateTime.now
		scheduledMeeting.creationDate = DateTime.now
		scheduledMeeting.format = format

		file.attached.asScala.foreach(attachment => {
			attachment.meetingRecord = scheduledMeeting
				scheduledMeeting.attachments.add(attachment)
				attachment.temporary = false
		})
		meetingRecordService.saveOrUpdate(scheduledMeeting)
		scheduledMeeting
	}

	def onBind(result: BindingResult) {
		file.onBind(result)
	}

}

trait CreateScheduledMeetingRecordCommandValidation extends SelfValidating with ScheduledMeetingRecordValidation  {
	self: CreateScheduledMeetingRecordState with MeetingRecordServiceComponent =>

	override def validate(errors: Errors) {

		meetingRecordService.listScheduled(Set(relationship), Some(creator)).foreach(
		 m => if (m.meetingDate == DateTimePickerFormatter.parseDateTime(meetingDateStr+" "+meetingTimeStr)) errors.rejectValue("meetingDate", "meetingRecord.date.duplicate")
		)
		if(DateTimePickerFormatter.parseDateTime(meetingDateStr+" "+meetingTimeStr).compareTo(DateTimePickerFormatter.parseDateTime(meetingDateStr+" "+meetingEndTimeStr)) > -1){
			errors.rejectValue("meetingTimeStr", "meetingRecord.date.endbeforestart")
		}
		if(DateTimePickerFormatter.parseDateTime(meetingDateStr+" "+meetingTimeStr).compareTo(DateTime.now) <= 0){
			errors.rejectValue("meetingDateStr", "meetingRecord.date.scheduledforpast")
		}
	}
}

trait CreateScheduledMeetingRecordState {
	def creator: Member
	def relationship: StudentRelationship
	def considerAlternatives: Boolean

	var title: String = _
	var description: String = _

	var meetingDateStr: String = _
	var meetingTimeStr: String = _
	var meetingEndTimeStr: String = _

	var format: MeetingFormat = _

	var meetingLocation: String  = _
	var meetingLocationId: String = _

	var file: UploadedFile = new UploadedFile
	var attachedFiles:JList[FileAttachment] = _

	var attachmentTypes: Seq[String] = Seq[String]()
}

trait CreateScheduledMeetingPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: CreateScheduledMeetingRecordState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.ScheduledMeetingRecord.Manage(relationship.relationshipType), mandatory(relationship.studentMember))
	}
}

trait CreateScheduledMeetingRecordDescription extends Describable[ScheduledMeetingRecord] {
	self: CreateScheduledMeetingRecordState =>

	override lazy val eventName = "CreateScheduledMeetingRecord"

	override def describe(d: Description) {
		relationship.studentMember.map { d.member(_) }
		d.properties(
			"creator" -> creator.universityId,
			"relationship" -> relationship.relationshipType.toString()
		)
	}
}

trait CreateScheduledMeetingRecordNotification extends Notifies[ScheduledMeetingRecord, ScheduledMeetingRecord] {
	def emit(meeting: ScheduledMeetingRecord): Seq[ScheduledMeetingRecordNotification with SingleRecipientNotification with AddsIcalAttachmentToScheduledMeetingNotification] = {
		val user = meeting.creator.asSsoUser
		val inviteeNotification = Notification.init(new ScheduledMeetingRecordInviteeNotification("created"), user, meeting, meeting.relationship)
		if(!meeting.universityIdInRelationship(user.getWarwickId)) {
			val behalfNotification = Notification.init(new ScheduledMeetingRecordBehalfNotification("created"), user, meeting, meeting.relationship)
			Seq(inviteeNotification, behalfNotification)
		} else {
			Seq(inviteeNotification)
		}
	}
}

trait CreateScheduledMeetingRecordScheduledNotifications extends SchedulesNotifications[ScheduledMeetingRecord, ScheduledMeetingRecord] {

	override def transformResult(meetingRecord: ScheduledMeetingRecord) = Seq(meetingRecord)

	override def scheduledNotifications(meetingRecord: ScheduledMeetingRecord): Seq[ScheduledNotification[ScheduledMeetingRecord]] = {
		Seq(
			new ScheduledNotification[ScheduledMeetingRecord]("ScheduledMeetingRecordReminderStudent", meetingRecord, meetingRecord.meetingDate.withTimeAtStartOfDay),
			new ScheduledNotification[ScheduledMeetingRecord]("ScheduledMeetingRecordReminderAgent", meetingRecord, meetingRecord.meetingDate.withTimeAtStartOfDay),
			new ScheduledNotification[ScheduledMeetingRecord]("ScheduledMeetingRecordConfirm", meetingRecord, meetingRecord.meetingDate),
			new ScheduledNotification[ScheduledMeetingRecord]("ScheduledMeetingRecordConfirm", meetingRecord, meetingRecord.meetingDate.plusDays(5))
		)
	}

}
