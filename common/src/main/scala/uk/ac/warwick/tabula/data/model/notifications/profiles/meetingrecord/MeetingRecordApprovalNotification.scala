package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}

import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.NotificationPriority.{Critical, Warning}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User

abstract class MeetingRecordApprovalNotification(val verb: String)
	extends Notification[MeetingRecord, Unit]
	with MeetingRecordNotificationTrait
	with SingleItemNotification[MeetingRecord]
	with AllCompletedActionRequiredNotification {

	override def onPreSave(newRecord: Boolean) {
		// if the meeting took place more than a week ago then this is more important
		priority = if (meeting.meetingDate.isBefore(DateTime.now.minusWeeks(1))) {
			Critical
		} else {
			Warning
		}
	}

	def meeting: MeetingRecord = item.entity

	def titleSuffix: String = "needs review"

	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> meeting.creator.asSsoUser,
		"agentRoles" -> agentRoles,
		"dateFormatter" -> dateOnlyFormatter,
		"verbed" ->  (if (verb == "create") "created" else "edited"),
		"meetingRecord" -> meeting,
	))
	def recipients: List[User] = meeting.pendingApprovers.map(_.asSsoUser)
	def urlTitle = "review the meeting record"

}

@Entity
@DiscriminatorValue("newMeetingRecordApproval")
class NewMeetingRecordApprovalNotification extends MeetingRecordApprovalNotification("create")

@Entity
@DiscriminatorValue("editedMeetingRecordApproval")
class EditedMeetingRecordApprovalNotification extends MeetingRecordApprovalNotification("edit")