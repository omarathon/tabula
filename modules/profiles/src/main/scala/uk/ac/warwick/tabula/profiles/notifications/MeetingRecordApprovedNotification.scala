package uk.ac.warwick.tabula.profiles.notifications

import uk.ac.warwick.tabula.data.model.MeetingRecordApproval

class MeetingRecordApprovedNotification(approval: MeetingRecordApproval)
	extends MeetingRecordNotification(approval.meetingRecord){

	override val agent = approval.approver.asSsoUser
	val verb = "approve"

	def title = "Meeting record approved"
	def content = renderToString(FreemarkerTemplate, Map(
		"actor" -> agent,
		"role"->actorRole,
		"dateFormatter" -> dateFormatter,
		"meetingRecord" -> approval.meetingRecord,
		"verbed" -> "approved",
		"nextActionDescription" -> "if you wish to view it",
		"profileLink" -> url
	))
	def recipients = Seq(approval.meetingRecord.creator.asSsoUser)
}

