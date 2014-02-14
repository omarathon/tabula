package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{SingleItemNotification, StudentRelationship, NotificationWithTarget, FreemarkerModel, MeetingRecord}
import javax.persistence.{Entity, DiscriminatorValue}

abstract class MeetingRecordApprovalNotification(val verb: String)
	extends NotificationWithTarget[MeetingRecord, StudentRelationship]
	with MeetingRecordNotificationTrait
	with SingleItemNotification[MeetingRecord] {

	//override val agent = meeting.creator.asSsoUser

	def meeting = item.entity
	def relationship = target.entity

	def title = "Meeting record approval required"
	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> agent,
		"role"->agentRole,
		"dateFormatter" -> dateOnlyFormatter,
		"verbed" ->  (if (verb == "create") "created" else "edited"),
		"nextActionDescription" -> "approve or reject it",
		"meetingRecord" -> meeting,
		"profileLink" -> url
	))
	def recipients = meeting.pendingApprovers.map(_.asSsoUser)
}

@Entity
@DiscriminatorValue("newMeetingRecordApproval")
class NewMeetingRecordApprovalNotification extends MeetingRecordApprovalNotification("create")

@Entity
@DiscriminatorValue("editedMeetingRecordApproval")
class EditedMeetingRecordApprovalNotification extends MeetingRecordApprovalNotification("edit")