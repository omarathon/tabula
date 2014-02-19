package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{FreemarkerModel, SingleRecipientNotification}
import javax.persistence.{DiscriminatorValue, Entity}

@Entity
@DiscriminatorValue(value="ScheduledMeetingRecordMissedInvitee")
class ScheduledMeetingRecordMissedInviteeNotification
	extends ScheduledMeetingRecordNotification with SingleRecipientNotification {

	verbSetting.value = "missed"

	def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/scheduled_meeting_record_missed_invitee_notification.ftl"
	def title = s"Scheduled meeting missed"
	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> agent,
		"role" -> agentRole,
		"dateTimeFormatter" -> dateTimeFormatter,
		"meetingRecord" -> meeting,
		"profileLink" -> url
	))
	def recipient = {
		if (meeting.creator.universityId == meeting.relationship.studentId) {
			meeting.relationship.agentMember.getOrElse(throw new IllegalStateException(agentNotFoundMessage)).asSsoUser
		} else {
			meeting.relationship.studentMember.getOrElse(throw new IllegalStateException(studentNotFoundMessage)).asSsoUser
		}
	}
}
