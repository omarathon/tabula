package uk.ac.warwick.tabula.data.model.notifications.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}

import uk.ac.warwick.tabula.data.model.{FreemarkerModel, MeetingRecordApproval, Notification, SingleItemNotification}

@Entity
@DiscriminatorValue("meetingRecordApproved")
class MeetingRecordApprovedNotification
	extends Notification[MeetingRecordApproval, Unit]
	with MeetingRecordNotificationTrait
	with SingleItemNotification[MeetingRecordApproval] {

	def approval = item.entity
	def meeting = approval.meetingRecord
	def relationship = meeting.relationship

	def verb = "approve"
	def actionRequired = false

	def title = "Meeting record approved"
	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> agent,
		"role"->agentRole,
		"dateFormatter" -> dateOnlyFormatter,
		"meetingRecord" -> approval.meetingRecord,
		"verbed" -> "approved"
	))

	def urlTitle = "view the meeting record"

	def recipients = Seq(approval.meetingRecord.creator.asSsoUser)
}

