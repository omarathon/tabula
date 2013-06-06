package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.data.model.MeetingRecord

class MeetingRecordApprovalNotification(meeting: MeetingRecord, val verb: String)
	extends MeetingRecordNotification(meeting)  {

	val actor = meeting.creator.asSsoUser

	def title = "Meeting record approval required"
	def content = renderToString(FreemarkerTemplate, Map(
		"actor" -> actor,
		"dateFormatter" -> dateFormatter,
		"verbed" ->  (if (verb == "create") "created" else "edited"),
		"nextActionDescription" -> "approve or reject it",
		"meetingRecord" -> meeting,
		"profileLink" -> url
	))
	def recipients = meeting.pendingApprovers.map(_.asSsoUser)
}
