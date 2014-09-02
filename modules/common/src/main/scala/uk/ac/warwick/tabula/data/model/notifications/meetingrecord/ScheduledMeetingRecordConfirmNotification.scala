package uk.ac.warwick.tabula.data.model.notifications.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.Days
import uk.ac.warwick.tabula.data.model.NotificationPriority.{Critical, Warning}
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, SingleRecipientNotification}
import uk.ac.warwick.userlookup.User

@Entity
@DiscriminatorValue(value="ScheduledMeetingRecordConfirm")
class ScheduledMeetingRecordConfirmNotification extends ScheduledMeetingRecordNotification with SingleRecipientNotification {

	verbSetting.value = "confirm"

	override final def onPreSave(newRecord: Boolean) {
		priority = if (Days.daysBetween(meeting.meetingDate, created).getDays >= 5) {
			Critical
		} else {
			Warning
		}
	}

	def actionRequired = true

	def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meetingrecord/scheduled_meeting_record_confirm_notification.ftl"

	def title = s"${agentRole.capitalize} meeting needs confirmation"

	override def urlTitle = "confirm whether this meeting took place"

	def isAgent = meeting.creator == meeting.relationship.agentMember.getOrElse(throw new IllegalStateException(agentNotFoundMessage))

	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"isAgent" -> isAgent,
		"partner" -> (isAgent match {
			case true => meeting.relationship.studentMember.getOrElse(throw new IllegalStateException(studentNotFoundMessage))
			case false => meeting.relationship.agentMember.getOrElse(throw new IllegalStateException(agentNotFoundMessage))
		}),
		"role" -> agentRole,
		"dateTimeFormatter" -> dateTimeFormatter,
		"meetingRecord" -> meeting
	))

	override def recipient: User = meeting.creator.asSsoUser
}
