package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, MyWarwickActivity, SingleRecipientNotification}
import uk.ac.warwick.userlookup.User

@Entity
@DiscriminatorValue(value="ScheduledMeetingRecordMissedInvitee")
class ScheduledMeetingRecordMissedInviteeNotification
	extends ScheduledMeetingRecordNotification
	with MyWarwickActivity {

	verbSetting.value = "missed"
	priority = Warning

	def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meetingrecord/scheduled_meeting_record_missed_invitee_notification.ftl"

	def title: String = s"Scheduled meeting with ${meeting.allParticipantNames} did not take place"

	override def titleFor(user: User): String = s"Scheduled meeting with ${meeting.participantNamesExcept(user)} did not take place"

	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> agent,
		"agentRoles" -> agentRoles,
		"dateTimeFormatter" -> dateTimeFormatter,
		"meetingRecord" -> meeting
	))

	override def recipients: Seq[User] = meeting.participants.filterNot(_ == meeting.creator).map(_.asSsoUser)
}
