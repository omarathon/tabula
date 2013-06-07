package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.data.model.{MeetingRecord, Notification}
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.spring.Wire
import freemarker.template.Configuration
import uk.ac.warwick.tabula.profiles.web.Routes

abstract class MeetingRecordNotification(meeting: MeetingRecord)
	extends Notification[MeetingRecord] with FreemarkerRendering {
	implicit var freemarker = Wire.auto[Configuration]

	val FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meeting_record_notification_template.ftl"
	val target = Some(meeting.relationship)
	val _object = meeting
	def url = Routes.profile.view(meeting.relationship.studentMember, meeting)
}
