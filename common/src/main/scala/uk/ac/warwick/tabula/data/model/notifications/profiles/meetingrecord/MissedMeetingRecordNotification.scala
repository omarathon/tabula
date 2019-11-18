package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User

trait MissedMeetingRecordNotification
  extends Notification[MeetingRecord, Unit]
    with MeetingRecordNotificationTrait
    with SingleItemNotification[MeetingRecord] {
  self: MyWarwickDiscriminator =>

  override def FreemarkerTemplate: String = "/WEB-INF/freemarker/notifications/meetingrecord/missed_meeting_record_notification.ftl"

  def meeting: MeetingRecord = item.entity

  def verb = "view"

  override def titleSuffix: String = "recorded as missed"

  override def title: String = "Missed meeting recorded"

  override def titleFor(user: User): String = "Missed meeting recorded"

  def content = FreemarkerModel(FreemarkerTemplate, Map(
    "actor" -> agent,
    "agentRoles" -> agentRoles,
    "dateFormatter" -> dateTimeFormatter,
    "student" -> meeting.student,
    "actorIsRecipient" -> recipients.contains(agent),
    "studentIsActor" -> (meeting.student.asSsoUser == agent),
    "studentIsRecipient" -> recipients.contains(meeting.student.asSsoUser),
    "meetingRecord" -> meeting
  ))

  def urlTitle = "view the meeting record"
}

@Entity
@Proxy
@DiscriminatorValue("missedMeetingRecordStudent")
class MissedMeetingRecordStudentNotification
  extends MissedMeetingRecordNotification with MyWarwickNotification {

  priority = NotificationPriority.Warning

  def recipients: Seq[User] = Seq(meeting.student.asSsoUser)
}

@Entity
@Proxy
@DiscriminatorValue("missedMeetingRecordAgent")
class MissedMeetingRecordAgentNotification
  extends MissedMeetingRecordNotification
    with MyWarwickNotification {

  priority = NotificationPriority.Info

  def recipients: Seq[User] = meeting.agents.map(_.asSsoUser)
}
