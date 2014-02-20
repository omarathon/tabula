package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{SingleItemNotification, Notification, FreemarkerModel, MeetingRecordApproval}
import javax.persistence.{Entity, DiscriminatorValue}

@Entity
@DiscriminatorValue("meetingRecordRejected")
class MeetingRecordRejectedNotification
	extends Notification[MeetingRecordApproval, Unit]
	with MeetingRecordNotificationTrait
	with SingleItemNotification[MeetingRecordApproval] {

	def approval = item.entity
	def meeting = approval.meetingRecord
	def relationship = meeting.relationship

	def verb = "reject"

	def title = "Meeting record rejected"
	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> agent,
		"role"->agentRole,
		"dateFormatter" -> dateOnlyFormatter,
		"meetingRecord" -> approval.meetingRecord,
		"verbed" -> "rejected",
		"nextActionDescription" -> "edit the record and submit it for approval again",
		"reason" -> approval.comments,
		"profileLink" -> url
	))
	def recipients = Seq(approval.meetingRecord.creator.asSsoUser)
}
