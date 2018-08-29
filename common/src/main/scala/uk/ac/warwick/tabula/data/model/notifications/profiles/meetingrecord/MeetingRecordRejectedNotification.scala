package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User

@Entity
@DiscriminatorValue("meetingRecordRejected")
class MeetingRecordRejectedNotification
	extends Notification[MeetingRecordApproval, Unit]
	with MeetingRecordNotificationTrait
	with SingleItemNotification[MeetingRecordApproval]
	with AllCompletedActionRequiredNotification {

	priority = NotificationPriority.Warning

	def approval: MeetingRecordApproval = item.entity
	def meeting: MeetingRecord = approval.meetingRecord

	def verb = "return"

	def titleSuffix: String = "returned with comments"

	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> agent,
		"agentRoles" -> agentRoles,
		"dateFormatter" -> dateOnlyFormatter,
		"meetingRecord" -> approval.meetingRecord,
		"verbed" -> "returned",
		"reason" -> approval.comments
	))

	def urlTitle = "edit the record and submit it for approval again"

	def recipients = Seq(approval.meetingRecord.creator.asSsoUser)
}
