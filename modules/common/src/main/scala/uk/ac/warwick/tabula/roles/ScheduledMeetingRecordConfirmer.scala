package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.permissions.Permissions._

case class ScheduledMeetingRecordConfirmer(meeting: model.ScheduledMeetingRecord) extends BuiltInRole(ScheduledMeetingRecordConfirmerRoleDefinition, meeting)

case object ScheduledMeetingRecordConfirmerRoleDefinition extends UnassignableBuiltInRoleDefinition {
	override def description = "Scheduled Meeting Record Confirmer"

	GrantsScopedPermission(
		Profiles.ScheduledMeetingRecord.Confirm
	)
}
