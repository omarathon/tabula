package uk.ac.warwick.tabula.data.model.notifications

import uk.ac.warwick.tabula.data.model.{NotificationWithTarget, SingleItemNotification, StudentRelationship, ScheduledMeetingRecord}
import uk.ac.warwick.tabula.profiles.web.Routes

abstract class ScheduledMeetingRecordNotification
	extends NotificationWithTarget[ScheduledMeetingRecord, StudentRelationship]
	with SingleItemNotification[ScheduledMeetingRecord] {

	def meeting = item.entity

	def verbSetting = StringSetting("verb")
	def verb = verbSetting.value.get

	def studentNotFoundMessage = "Student member for SCJ code " + meeting.relationship.studentCourseDetails.scjCode + " not found"
	def agentNotFoundMessage = "Agent member for code " + meeting.relationship.agent + " not found"
	def url = Routes.profile.view(
			meeting.relationship.studentMember.getOrElse(throw new IllegalStateException(studentNotFoundMessage)),
			meeting
	)
	def urlTitle = "view this meeting"

	def agentRole = meeting.relationship.relationshipType.agentRole
}
