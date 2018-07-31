package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User

trait MissedMeetingRecordNotification
	extends Notification[MeetingRecord, Unit]
		with MeetingRecordNotificationTrait
		with SingleItemNotification[MeetingRecord] {
	self: MyWarwickDiscriminator =>

	override def FreemarkerTemplate: String = "/WEB-INF/freemarker/notifications/meetingrecord/missed_meeting_record_notification.ftl"

	def meeting: MeetingRecord = item.entity

	def relationship: StudentRelationship = meeting.relationship

	def verb = "view"

	def title: String = "Missed meeting recorded"

	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> agent,
		"role" -> agentRole,
		"dateFormatter" -> dateTimeFormatter,
		"student" -> relationship.studentMember.orNull,
		"actorIsRecipient" -> recipients.contains(agent),
		"studentIsRecipient" -> relationship.studentMember.map(_.asSsoUser).exists(recipients.contains),
		"meetingRecord" -> meeting
	))

	def urlTitle = "view the meeting record"
}

@Entity
@DiscriminatorValue("missedMeetingRecordStudent")
class MissedMeetingRecordStudentNotification
	extends MissedMeetingRecordNotification with MyWarwickNotification {

	priority = NotificationPriority.Warning

	def recipients: Seq[User] = relationship.studentMember.map(_.asSsoUser).toSeq
}

@Entity
@DiscriminatorValue("missedMeetingRecordAgent")
class MissedMeetingRecordAgentNotification
	extends MissedMeetingRecordNotification
		with MyWarwickActivity {

	priority = NotificationPriority.Info

	def recipients: Seq[User] = relationship.agentMember.map(_.asSsoUser).toSeq
}
