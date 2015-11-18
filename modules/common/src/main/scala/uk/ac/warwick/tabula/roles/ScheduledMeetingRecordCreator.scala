package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.permissions.Permissions._

case class ScheduledMeetingRecordCreator(meeting: model.ScheduledMeetingRecord, relationshipType: StudentRelationshipType) extends BuiltInRole(ScheduledMeetingRecordCreatorRoleDefinition(relationshipType), meeting)

case class ScheduledMeetingRecordCreatorRoleDefinition(relationshipType: StudentRelationshipType) extends UnassignableBuiltInRoleDefinition {
	override def description = "Scheduled Meeting Record Creator"

	GrantsScopedPermission(
		Profiles.ScheduledMeetingRecord.Manage(relationshipType),
		Profiles.ScheduledMeetingRecord.Confirm
	)
}
