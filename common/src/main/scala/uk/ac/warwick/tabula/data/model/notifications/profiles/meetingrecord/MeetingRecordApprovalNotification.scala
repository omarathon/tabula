package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}
import org.hibernate.annotations.Proxy
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.NotificationPriority.{Critical, Warning}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User

abstract class MeetingRecordApprovalNotification(@transient val verb: String)
  extends BatchedNotification[MeetingRecord, Unit, Notification[_, Unit] with MeetingRecordNotificationTrait](MeetingRecordBatchedNotificationHandler)
    with MeetingRecordNotificationTrait
    with SingleItemNotification[MeetingRecord]
    with AllCompletedActionRequiredNotification {

  override def onPreSave(newRecord: Boolean): Unit = {
    // if the meeting took place more than a week ago then this is more important
    priority = if (meeting.meetingDate.isBefore(DateTime.now.minusWeeks(1))) {
      Critical
    } else {
      Warning
    }
  }

  def meeting: MeetingRecord = item.entity

  override def verbed: String = if (verb == "create") "created" else "edited"

  def titleSuffix: String = "needs review"

  def content = FreemarkerModel(FreemarkerTemplate, Map(
    "actor" -> meeting.creator.asSsoUser,
    "agentRoles" -> agentRoles,
    "dateTimeFormatter" -> dateTimeFormatter,
    "verbed" -> verbed,
    "meetingRecord" -> meeting,
  ))

  def recipients: List[User] = meeting.pendingApprovers.map(_.asSsoUser)

  def urlTitle = "review the meeting record"

}

@Entity
@Proxy
@DiscriminatorValue("newMeetingRecordApproval")
class NewMeetingRecordApprovalNotification extends MeetingRecordApprovalNotification("create")

@Entity
@Proxy
@DiscriminatorValue("editedMeetingRecordApproval")
class EditedMeetingRecordApprovalNotification extends MeetingRecordApprovalNotification("edit")
