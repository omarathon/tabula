package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import uk.ac.warwick.tabula.data.model.{MeetingRecord, StudentRelationship}
import uk.ac.warwick.tabula.profiles.web.Routes

trait MeetingRecordNotificationTrait {

	def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meetingrecord/meeting_record_notification_template.ftl"

	def meeting: MeetingRecord
	def relationship: StudentRelationship

	def url = Routes.Profile.relationshipType(
		meeting.relationship.studentMember.getOrElse(throw new IllegalStateException(s"Student member for relationship ${meeting.relationship.id} not found")),
		relationship.relationshipType
	)

	def agentRole = meeting.relationship.relationshipType.agentRole
}
