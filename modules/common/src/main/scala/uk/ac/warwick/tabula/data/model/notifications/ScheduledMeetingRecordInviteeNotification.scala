package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{FreemarkerModel, SingleRecipientNotification}
import javax.persistence.{DiscriminatorValue, Entity}

@Entity
@DiscriminatorValue(value="ScheduledMeetingRecordInvitee")
class ScheduledMeetingRecordInviteeNotification
	extends ScheduledMeetingRecordNotification with SingleRecipientNotification {

	def this(theVerb: String) {
		this()
		verbSetting.value = theVerb
	}

	def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/scheduled_meeting_record_invitee_notification.ftl"
	def title = s"Scheduled meeting $verb"
	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> agent,
		"role" -> agentRole,
		"verb" -> verb,
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
