package uk.ac.warwick.tabula.data.model.notifications.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model.{FreemarkerModel, SingleRecipientNotification}

@Entity
@DiscriminatorValue(value="ScheduledMeetingRecordBehalf")
class ScheduledMeetingRecordBehalfNotification
	extends ScheduledMeetingRecordNotification with SingleRecipientNotification
	with AddsIcalAttachmentToScheduledMeetingNotification {

	def this(theVerb: String) {
		this()
		verbSetting.value = theVerb
	}

	def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meetingrecord/scheduled_meeting_record_behalf_notification.ftl"
	def title = s"Meeting $verb on your behalf by ${agent.getFullName}"

	def student = meeting.relationship.studentMember.getOrElse(throw new IllegalStateException(studentNotFoundMessage)).asSsoUser

	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> agent,
		"student" -> student,
		"role" -> agentRole,
		"verb" -> verb,
		"dateTimeFormatter" -> dateTimeFormatter,
		"meetingRecord" -> meeting
	))

	def recipient = meeting.relationship.agentMember.getOrElse(throw new IllegalStateException(agentNotFoundMessage)).asSsoUser

	def actionRequired = false
}
