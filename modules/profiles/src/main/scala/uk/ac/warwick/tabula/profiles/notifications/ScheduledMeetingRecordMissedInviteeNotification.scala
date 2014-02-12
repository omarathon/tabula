package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.data.model.{SingleRecipientNotification, ScheduledMeetingRecord}

class ScheduledMeetingRecordMissedInviteeNotification(meeting: ScheduledMeetingRecord, val verb: String)
	extends ScheduledMeetingRecordNotification(meeting) with SingleRecipientNotification {

	override val agent = meeting.creator.asSsoUser

	val FreemarkerTemplate = "/WEB-INF/freemarker/notifications/scheduled_meeting_record_missed_invitee_notification.ftl"
	def title = s"Scheduled meeting missed"
	def content = renderToString(FreemarkerTemplate, Map(
		"actor" -> agent,
		"role" -> agentRole,
		"dateTimeFormatter" -> dateTimeFormatter,
		"meetingRecord" -> meeting,
		"profileLink" -> url
	))
	val recipient = {
		if (meeting.creator.universityId == meeting.relationship.studentId ) {
			meeting.relationship.studentMember.getOrElse(throw new IllegalStateException(studentNotFoundMessage)).asSsoUser
		}
		else meeting.relationship.agentMember.getOrElse(throw new IllegalStateException(agentNotFoundMessage)).asSsoUser
	}
}
