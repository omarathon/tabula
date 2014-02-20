package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{StudentRelationship, MeetingRecord}
import uk.ac.warwick.tabula.profiles.web.Routes

trait MeetingRecordNotificationTrait {

	def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meeting_record_notification_template.ftl"

	def meeting: MeetingRecord
	def relationship: StudentRelationship

	def url = Routes.profile.view(
			meeting.relationship.studentMember.getOrElse(throw new IllegalStateException(s"Student member for relationship ${meeting.relationship.id} not found")), 
			meeting)

	def agentRole = meeting.relationship.relationshipType.agentRole
}
