package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.Days
import uk.ac.warwick.tabula.data.model.NotificationPriority.{Critical, Warning}
import uk.ac.warwick.tabula.data.model.{AllCompletedActionRequiredNotification, ActionRequiredNotification, FreemarkerModel, SingleRecipientNotification}
import uk.ac.warwick.userlookup.User

@Entity
@DiscriminatorValue(value="ScheduledMeetingRecordConfirm")
class ScheduledMeetingRecordConfirmNotification
	extends ScheduledMeetingRecordNotification
	with SingleRecipientNotification
	with AllCompletedActionRequiredNotification {

	verbSetting.value = "confirm"

	override final def onPreSave(newRecord: Boolean) {
		priority = if (Days.daysBetween(meeting.meetingDate, created).getDays >= 5) {
			Critical
		} else {
			Warning
		}
	}

	def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meetingrecord/scheduled_meeting_record_confirm_notification.ftl"

	def title: String = s"Meeting record with ${meeting.allParticipantNames} needs confirmation"

	override def titleFor(user: User): String = s"Meeting record with ${meeting.participantNamesExcept(user)} needs confirmation"

	override def urlTitle = "confirm whether this meeting took place"

	def isAgent: Boolean = meeting.agents.contains(meeting.creator)

	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"isAgent" -> isAgent,
		"otherParticipants" -> meeting.participants.filterNot(_ == meeting.creator),
		"agentRoles" -> agentRoles,
		"dateTimeFormatter" -> dateTimeFormatter,
		"meetingRecord" -> meeting
	))

	override def recipient: User = meeting.creator.asSsoUser
}
