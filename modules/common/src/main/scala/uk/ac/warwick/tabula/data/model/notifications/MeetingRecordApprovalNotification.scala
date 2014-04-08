package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{SingleItemNotification, StudentRelationship, NotificationWithTarget, FreemarkerModel, MeetingRecord}
import javax.persistence.{Entity, DiscriminatorValue}
import uk.ac.warwick.tabula.data.model.NotificationPriority.{Critical, Warning}
import uk.ac.warwick.tabula.data.PreSaveBehaviour
import org.joda.time.DateTime

abstract class MeetingRecordApprovalNotification(val verb: String)
	extends NotificationWithTarget[MeetingRecord, StudentRelationship]
	with MeetingRecordNotificationTrait
	with SingleItemNotification[MeetingRecord]
	with PreSaveBehaviour {

	override def preSave(newRecord: Boolean) {
		// if the meeting took place more than a week ago then this is more important
		priority = if (meeting.meetingDate.isBefore(DateTime.now.minusWeeks(1))) {
			Critical
		} else {
			Warning
		}
	}

	def meeting = item.entity
	def relationship = target.entity

	def title = "Meeting record approval required"
	def actionRequired = true
	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> agent,
		"role"->agentRole,
		"dateFormatter" -> dateOnlyFormatter,
		"verbed" ->  (if (verb == "create") "created" else "edited"),
		"meetingRecord" -> meeting
	))
	def recipients = meeting.pendingApprovers.map(_.asSsoUser)
	def urlTitle = "approve or reject the meeting record"

}

@Entity
@DiscriminatorValue("newMeetingRecordApproval")
class NewMeetingRecordApprovalNotification extends MeetingRecordApprovalNotification("create")

@Entity
@DiscriminatorValue("editedMeetingRecordApproval")
class EditedMeetingRecordApprovalNotification extends MeetingRecordApprovalNotification("edit")