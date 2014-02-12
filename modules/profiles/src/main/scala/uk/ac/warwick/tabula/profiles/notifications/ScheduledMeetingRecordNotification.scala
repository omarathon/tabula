package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.data.model.{StudentRelationship, ScheduledMeetingRecord, Notification}
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.spring.Wire
import freemarker.template.Configuration
import uk.ac.warwick.tabula.profiles.web.Routes

abstract class ScheduledMeetingRecordNotification(meeting: ScheduledMeetingRecord)
	extends Notification[ScheduledMeetingRecord] with FreemarkerRendering {
	implicit var freemarker = Wire.auto[Configuration]

	val target: Option[StudentRelationship] = Some(meeting.relationship)
	val _object = meeting
	var studentNotFoundMessage = "Student member for SCJ code " + meeting.relationship.studentCourseDetails.scjCode + " not found"
	var agentNotFoundMessage = "Agent member for code " + meeting.relationship.agent + " not found"
	def url = Routes.profile.view(
			meeting.relationship.studentMember.getOrElse(throw new IllegalStateException(studentNotFoundMessage)),
			meeting)

	def agentRole = meeting.relationship.relationshipType.agentRole
}
