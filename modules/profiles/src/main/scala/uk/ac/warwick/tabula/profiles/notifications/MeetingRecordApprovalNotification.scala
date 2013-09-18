package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.data.model.MeetingRecord

class MeetingRecordApprovalNotification(meeting: MeetingRecord, val verb: String)
	extends MeetingRecordNotification(meeting)  {

	override val agent = meeting.creator.asSsoUser

	def title = "Meeting record approval required"
	def content = renderToString(FreemarkerTemplate, Map(
		"actor" -> agent,
		"role"->agentRole,
		"dateFormatter" -> dateFormatter,
		"verbed" ->  (if (verb == "create") "created" else "edited"),
		"nextActionDescription" -> "approve or reject it",
		"meetingRecord" -> meeting,
		"profileLink" -> url
	))
	def recipients = meeting.pendingApprovers.map(_.asSsoUser)
}
