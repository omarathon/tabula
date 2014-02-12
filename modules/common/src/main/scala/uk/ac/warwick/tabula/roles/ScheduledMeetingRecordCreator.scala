package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._

/*
 * Allow creators of scheduled meetings to update/remove the meeting
 */
case class ScheduledMeetingRecordCreator(meeting: model.ScheduledMeetingRecord) extends BuiltInRole(ScheduledMeetingRecordCreatorRoleDefinition, meeting)

case object ScheduledMeetingRecordCreatorRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Scheduled Meeting Record Creator"

	GrantsScopedPermission(
		Profiles.ScheduledMeetingRecord.Update,
		Profiles.ScheduledMeetingRecord.Delete
	)

}
