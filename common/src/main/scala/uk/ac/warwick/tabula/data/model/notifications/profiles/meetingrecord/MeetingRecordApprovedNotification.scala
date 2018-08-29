package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import javax.persistence.{DiscriminatorValue, Entity}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.userlookup.User

@Entity
@DiscriminatorValue("meetingRecordApproved")
class MeetingRecordApprovedNotification
	extends Notification[MeetingRecordApproval, Unit]
	with MeetingRecordNotificationTrait
	with SingleItemNotification[MeetingRecordApproval]
	with MyWarwickActivity {

	def approval: MeetingRecordApproval = item.entity
	def meeting: MeetingRecord = approval.meetingRecord

	def verb = "approve"

	override def titleSuffix: String = "approved"

	def content = FreemarkerModel(FreemarkerTemplate, Map(
		"actor" -> agent,
		"agentRoles" -> agentRoles,
		"dateFormatter" -> dateOnlyFormatter,
		"meetingRecord" -> approval.meetingRecord,
		"verbed" -> "approved"
	))

	def urlTitle = "view the meeting record"

	def recipients: Seq[User] = Seq(meeting.creator, meeting.student).filterNot(_.asSsoUser == agent).distinct.map(_.asSsoUser)
}

