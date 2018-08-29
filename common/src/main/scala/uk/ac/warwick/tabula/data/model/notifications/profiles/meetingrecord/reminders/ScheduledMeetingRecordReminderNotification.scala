package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.reminders

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.NotificationPriority.Warning
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.ScheduledMeetingRecordNotification
import uk.ac.warwick.tabula.data.model.{FreemarkerModel, MyWarwickNotification, SingleRecipientNotification}
import uk.ac.warwick.tabula.helpers.ConfigurableIntervalFormatter
import uk.ac.warwick.userlookup.User

abstract class ScheduledMeetingRecordReminderNotification extends ScheduledMeetingRecordNotification
	with MyWarwickNotification {

	verbSetting.value = "remind"
	priority = Warning

	def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meetingrecord/scheduled_meeting_record_reminder_notification.ftl"

	def referenceDate: DateTime = meeting.meetingDate
	def isToday: Boolean = {
		val today = DateTime.now.withTimeAtStartOfDay()
		val meetingDate = meeting.meetingDate.withTimeAtStartOfDay()
		today == meetingDate
	}

	def dateForTitle: String = {
		val timeFormat = ConfigurableIntervalFormatter.Hour12OptionalMins

		if (isToday) s"today at ${timeFormat.formatTime(meeting.meetingDate).get}"
		else s"at ${timeFormat.formatTime(meeting.meetingDate).get}, ${meeting.meetingDate.toString("EEEE d MMMM yyyy")}"
	}

	def title: String = s"Meeting with ${meeting.allParticipantNames} $dateForTitle"

	override def titleFor(user: User): String = s"Meeting with ${meeting.participantNamesExcept(user)} $dateForTitle"

	def isAgent: Boolean

	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"isAgent" -> isAgent,
		"agentRoles" -> agentRoles,
		"otherParticipants" -> meeting.participants.filterNot(_ == meeting.creator),
		"dateTimeFormatter" -> dateTimeFormatter,
		"meetingRecord" -> meeting
	))
}

@Entity
@DiscriminatorValue(value="ScheduledMeetingRecordReminderStudent")
class ScheduledMeetingRecordReminderStudentNotification extends ScheduledMeetingRecordReminderNotification {

	override def isAgent = false

	override def recipients: Seq[User] = Seq(meeting.student.asSsoUser)

}

@Entity
@DiscriminatorValue(value="ScheduledMeetingRecordReminderAgent")
class ScheduledMeetingRecordReminderAgentNotification extends ScheduledMeetingRecordReminderNotification {

	override def isAgent = true

	override def recipients: Seq[User] = meeting.agents.map(_.asSsoUser)

}
