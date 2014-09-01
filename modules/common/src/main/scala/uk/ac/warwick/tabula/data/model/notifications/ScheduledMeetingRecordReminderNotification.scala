package uk.ac.warwick.tabula.data.model.notifications

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model.{SingleRecipientNotification, FreemarkerModel}
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning

abstract class ScheduledMeetingRecordReminderNotification extends ScheduledMeetingRecordNotification with SingleRecipientNotification {

	verbSetting.value = "remind"
	priority = Warning
	def actionRequired = false

	def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/scheduled_meeting_record_reminder_notification.ftl"

	def title = s"${agentRole.capitalize} meeting today"

	def isAgent: Boolean

	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"isAgent" -> isAgent,
		"role" -> agentRole,
		"partner" -> (isAgent match {
			case true => meeting.relationship.studentMember.getOrElse(throw new IllegalStateException(studentNotFoundMessage))
			case false => meeting.relationship.agentMember.getOrElse(throw new IllegalStateException(agentNotFoundMessage))
		}),
		"dateTimeFormatter" -> dateTimeFormatter,
		"meetingRecord" -> meeting,
		"url" -> url
	))
}

@Entity
@DiscriminatorValue(value="ScheduledMeetingRecordReminderStudent")
class ScheduledMeetingRecordReminderStudentNotification extends ScheduledMeetingRecordReminderNotification {

	override def isAgent = false

	override def recipient = meeting.relationship.studentMember.getOrElse(throw new IllegalStateException(studentNotFoundMessage)).asSsoUser

}

@Entity
@DiscriminatorValue(value="ScheduledMeetingRecordReminderAgent")
class ScheduledMeetingRecordReminderAgentNotification extends ScheduledMeetingRecordReminderNotification {

	override def isAgent = true

	override def recipient = meeting.relationship.agentMember.getOrElse(throw new IllegalStateException(agentNotFoundMessage)).asSsoUser

}
