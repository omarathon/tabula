package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import uk.ac.warwick.tabula.data.model._

@Entity
@Proxy
@DiscriminatorValue("meetingRecordRejected")
class MeetingRecordRejectedNotification
  extends BatchedNotification[MeetingRecordApproval, Unit, Notification[_, Unit] with MeetingRecordNotificationTrait](MeetingRecordBatchedNotificationHandler)
    with MeetingRecordNotificationTrait
    with SingleItemNotification[MeetingRecordApproval]
    with AllCompletedActionRequiredNotification {

  priority = NotificationPriority.Warning

  def approval: MeetingRecordApproval = item.entity

  def meeting: MeetingRecord = approval.meetingRecord

  def verb = "return"

  override def verbed: String = "returned"

  def titleSuffix: String = "returned with comments"

  def content = FreemarkerModel(FreemarkerTemplate, Map(
    "actor" -> agent,
    "agentRoles" -> agentRoles,
    "dateTimeFormatter" -> dateTimeFormatter,
    "meetingRecord" -> approval.meetingRecord,
    "verbed" -> verbed,
    "reason" -> approval.comments
  ))

  def urlTitle = "edit the record and submit it for approval again"

  def recipients = Seq(approval.meetingRecord.creator.asSsoUser)
}
