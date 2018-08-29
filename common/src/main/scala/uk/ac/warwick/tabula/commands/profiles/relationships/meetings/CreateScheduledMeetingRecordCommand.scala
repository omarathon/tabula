package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.DateFormats.DateTimePickerFormatter
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.{AddsIcalAttachmentToScheduledMeetingNotification, ScheduledMeetingRecordBehalfNotification, ScheduledMeetingRecordInviteeNotification, ScheduledMeetingRecordNotification}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions._

import scala.collection.JavaConverters._

object CreateScheduledMeetingRecordCommand {
	def apply(creator: Member, studentCourseDetails: StudentCourseDetails, allRelationships: Seq[StudentRelationship]) =
		new CreateScheduledMeetingRecordCommand(creator, studentCourseDetails, allRelationships)
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

class CreateScheduledMeetingRecordCommand(val creator: Member, val studentCourseDetails: StudentCourseDetails, val allRelationships: Seq[StudentRelationship])
	extends CommandInternal[ScheduledMeetingRecord] with CreateScheduledMeetingRecordState with BindListener {

	self: MeetingRecordServiceComponent =>

	def applyInternal(): ScheduledMeetingRecord = {
		val scheduledMeeting = new ScheduledMeetingRecord(creator, relationships.asScala)
		scheduledMeeting.title = title
		scheduledMeeting.description = description

		if ((!meetingDateStr.isEmptyOrWhitespace) && (!meetingTimeStr.isEmptyOrWhitespace) && (!meetingEndTimeStr.isEmptyOrWhitespace)) {

			scheduledMeeting.meetingDate = DateTimePickerFormatter.parseDateTime(meetingDateStr+" "+meetingTimeStr)
			scheduledMeeting.meetingEndDate = DateTimePickerFormatter.parseDateTime(meetingDateStr+" "+meetingEndTimeStr)

		}

		scheduledMeeting.meetingLocation = if (meetingLocation.hasText) {
			if (meetingLocationId.hasText) {
				MapLocation(meetingLocation.toString, meetingLocationId)
			} else {
				NamedLocation(meetingLocation.toString)
			}
		} else {
			null
		}

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
		sharedValidation(errors, title, meetingDateStr, meetingTimeStr, meetingEndTimeStr, meetingLocation)

		meetingRecordService.listScheduled(relationships.asScala.toSet, Some(creator)).foreach(
			m =>
				if ((!meetingDateStr.isEmptyOrWhitespace) && (!meetingTimeStr.isEmptyOrWhitespace) && (!meetingEndTimeStr.isEmptyOrWhitespace)) {

					val dateCheck = meetingDateStr+" "+meetingTimeStr
					if (m.meetingDate.toString(DateTimePickerFormatter).equals(dateCheck)) errors.rejectValue("meetingDateStr", "meetingRecord.date.duplicate")
			}
		)

	}
}

trait CreateScheduledMeetingRecordState extends ModifyScheduledMeetingRecordState {
	def creator: Member
	def studentCourseDetails: StudentCourseDetails
	def allRelationships: Seq[StudentRelationship]
}

trait CreateScheduledMeetingPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: CreateScheduledMeetingRecordState =>

	override def permissionsCheck(p: PermissionsChecking) {
		allRelationships.map(_.relationshipType).foreach { relationshipType =>
			p.PermissionCheck(Permissions.Profiles.ScheduledMeetingRecord.Manage(relationshipType), mandatory(studentCourseDetails.student))
		}
	}
}

trait CreateScheduledMeetingRecordDescription extends Describable[ScheduledMeetingRecord] {
	self: CreateScheduledMeetingRecordState =>

	override lazy val eventName = "CreateScheduledMeetingRecord"

	override def describe(d: Description) {
		relationships.asScala.flatMap(_.studentMember).headOption.map(d.member)
		d.properties(
			"creator" -> creator.universityId,
			"relationship" -> relationships.asScala.map(_.relationshipType).distinct.mkString(", ")
		)
	}
}

trait CreateScheduledMeetingRecordNotification extends Notifies[ScheduledMeetingRecord, ScheduledMeetingRecord] {
	def emit(meeting: ScheduledMeetingRecord): Seq[ScheduledMeetingRecordNotification with AddsIcalAttachmentToScheduledMeetingNotification] = {
		val user = meeting.creator.asSsoUser
		val inviteeNotification = Notification.init(new ScheduledMeetingRecordInviteeNotification("created"), user, meeting)
		if(!meeting.universityIdInRelationship(user.getWarwickId)) {
			val behalfNotification = Notification.init(new ScheduledMeetingRecordBehalfNotification("created"), user, meeting)
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
