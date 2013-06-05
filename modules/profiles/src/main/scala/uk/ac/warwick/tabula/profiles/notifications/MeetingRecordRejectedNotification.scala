package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.data.model.{Notification, MeetingRecordApproval, MeetingRecord}
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.spring.Wire
import freemarker.template.Configuration
import uk.ac.warwick.tabula.profiles.web.Routes


class MeetingRecordRejectedNotification(approval: MeetingRecordApproval) extends Notification[MeetingRecord]
	with FreemarkerRendering  {

	implicit var freemarker = Wire.auto[Configuration]

	val meeting = approval.meetingRecord

	val actor = approval.approver.asSsoUser
	val verb = "reject"
	val target = Some(meeting.relationship)
	val _object = meeting

	def title = "Meeting record rejected"
	def url = Routes.profile.view(meeting.relationship.studentMember, meeting)
	def content = renderToString("/WEB-INF/freemarker/notifications/meeting_record_notification_template.ftl", Map(
		"dateFormatter" -> dateFormatter,
		"meetingRecord" -> meeting,
		"verbed" -> "rejected",
		"nextActionDescription" -> "edit the record and submit it for approval again",
		"reason" -> approval.comments,
		"profileLink" -> url
	))
	def recipients = Seq(meeting.creator.asSsoUser)
}
