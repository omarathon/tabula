package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions.Profiles.MeetingRecord

case class MeetingRecordApprover(approval: model.MeetingRecordApproval) extends BuiltInRole(MeetingRecordApproverRoleDefinition, approval)

case object MeetingRecordApproverRoleDefinition extends UnassignableBuiltInRoleDefinition {
	override def description = "Approver"

	GrantsScopedPermission(
		MeetingRecord.Approve
	)
}