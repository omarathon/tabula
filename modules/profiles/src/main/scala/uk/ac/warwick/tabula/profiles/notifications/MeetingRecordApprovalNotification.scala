package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.data.model.{Notification, MeetingRecord}
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.spring.Wire
import freemarker.template.Configuration
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.DateFormats

class MeetingRecordApprovalNotification(meeting: MeetingRecord, _verb: String)
	extends Notification[MeetingRecord] with FreemarkerRendering  {

	implicit var freemarker = Wire.auto[Configuration]

	val actor = meeting.creator.asSsoUser
	val verb = _verb
	val target = Some(meeting.relationship)
	val _object = meeting

	def title = "Meeting record approval required"
	def url = Routes.profile.view(meeting.relationship.studentMember, meeting)
	def content = renderToString("/WEB-INF/freemarker/notifications/meeting_record_approval_notification.ftl", Map(
		"dateFormatter" -> dateFormatter,
		"verbed" ->  (if (verb == "create") "created" else "edited"),
		"meetingRecord" -> meeting,
		"profileLink" -> url
	))
	def recipients = meeting.pendingApprovers.map(_.asSsoUser)
}
