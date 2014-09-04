package uk.ac.warwick.tabula.data.model.notifications.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, SingleRecipientNotification}

@Entity
@DiscriminatorValue(value="ScheduledMeetingRecordMissedInvitee")
class ScheduledMeetingRecordMissedInviteeNotification
	extends ScheduledMeetingRecordNotification with SingleRecipientNotification {

	verbSetting.value = "missed"
	priority = Warning
	def actionRequired = false

	def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meetingrecord/scheduled_meeting_record_missed_invitee_notification.ftl"
	def title = s"Scheduled personal tutor meeting did not take place"
	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> agent,
		"role" -> agentRole,
		"dateTimeFormatter" -> dateTimeFormatter,
		"meetingRecord" -> meeting
	))
	def recipient = {
		if (meeting.creator.universityId == meeting.relationship.studentId) {
			meeting.relationship.agentMember.getOrElse(throw new IllegalStateException(agentNotFoundMessage)).asSsoUser
		} else {
			meeting.relationship.studentMember.getOrElse(throw new IllegalStateException(studentNotFoundMessage)).asSsoUser
		}
	}
}
