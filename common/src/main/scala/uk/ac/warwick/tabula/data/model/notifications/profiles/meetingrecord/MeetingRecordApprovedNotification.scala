package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.userlookup.User

@Entity
@Proxy
@DiscriminatorValue("meetingRecordApproved")
class MeetingRecordApprovedNotification
  extends BatchedNotification[MeetingRecordApproval, Unit, Notification[_, Unit] with MeetingRecordNotificationTrait](MeetingRecordBatchedNotificationHandler)
    with MeetingRecordNotificationTrait
    with SingleItemNotification[MeetingRecordApproval]
    with MyWarwickActivity {

  def approval: MeetingRecordApproval = item.entity

  def meeting: MeetingRecord = approval.meetingRecord

  def verb = "approve"

  override def verbed: String = "approved"

  override def titleSuffix: String = "approved"

  def content: FreemarkerModel = FreemarkerModel(FreemarkerTemplate, Map(
    "actor" -> agent,
    "agentRoles" -> agentRoles,
    "dateTimeFormatter" -> dateTimeFormatter,
    "meetingRecord" -> approval.meetingRecord,
    "verbed" -> verbed
  ))

  def urlTitle = "view the meeting record"

  def recipients: Seq[User] = Seq(meeting.creator, meeting.student).filterNot(_.asSsoUser == agent).distinct.map(_.asSsoUser)
}
