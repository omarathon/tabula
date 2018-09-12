package uk.ac.warwick.tabula.data.model

sealed abstract class MeetingRecordApprovalType(val code: String, val description: String)

object MeetingRecordApprovalType {
  case object AllApprovals extends MeetingRecordApprovalType(code = "all", description = "A meeting record is approved when everyone approves it")
	case object OneApproval extends MeetingRecordApprovalType(code = "one", description = "A meeting record is approved when one person approves it")

	def default: MeetingRecordApprovalType = AllApprovals
  def values: Seq[MeetingRecordApprovalType] = Seq(AllApprovals, OneApproval)

  def fromCode(code: String): Option[MeetingRecordApprovalType] = values.find(_.code == code)
}
