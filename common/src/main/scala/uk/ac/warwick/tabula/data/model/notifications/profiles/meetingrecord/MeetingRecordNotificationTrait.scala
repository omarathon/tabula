package uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{MeetingRecord, Notification}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.userlookup.User

trait MeetingRecordNotificationTrait {
	self: Notification[_, Unit] =>

	def FreemarkerTemplate = "/WEB-INF/freemarker/notifications/meetingrecord/meeting_record_notification_template.ftl"

	def meeting: MeetingRecord

	def academicYear: AcademicYear = AcademicYear.forDate(meeting.meetingDate)

	def url: String = Routes.Profile.relationshipType(
		meeting.relationships.head.studentCourseDetails,
		academicYear,
		meeting.relationships.head.relationshipType
	)

	def agentRoles: Seq[String] = meeting.relationships.map(_.relationshipType.agentRole).distinct.sorted

	override def title: String = meeting.relationshipTypes match {
		case Seq(relationshipType) =>
			s"${relationshipType.agentRole.capitalize} meeting with ${meeting.allParticipantNames} $titleSuffix"
		case _ =>
			s"Meeting record with ${meeting.allParticipantNames} $titleSuffix"
	}

	override def titleFor(user: User): String = meeting.relationshipTypes match {
		case Seq(relationshipType) =>
			s"${relationshipType.agentRole.capitalize} meeting record with ${meeting.participantNamesExcept(user)} $titleSuffix"
		case _ =>
			s"Meeting record with ${meeting.participantNamesExcept(user)} $titleSuffix"
	}

	def titleSuffix: String
}
