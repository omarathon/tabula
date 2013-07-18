package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.data._

case class Supervisor(student: model.Member) extends BuiltInRole(SupervisorRoleDefinition, student)

object SupervisorRoleDefinition extends BuiltInRoleDefinition {

	override def description = "Supervisor"

	GrantsScopedPermission(
		Profiles.Read.Core,
		Profiles.Read.NextOfKin,
		Profiles.Read.HomeAddress,
		Profiles.Read.TermTimeAddress,
		Profiles.Read.TelephoneNumber,
		Profiles.Read.MobileNumber,
		Profiles.Read.Usercode,
    Profiles.Supervisor.MeetingRecord.Create,
    Profiles.Supervisor.MeetingRecord.Read,
    Profiles.Supervisor.MeetingRecord.ReadDetails,
    Profiles.Supervisor.MeetingRecord.Update,
    Profiles.Supervisor.MeetingRecord.Delete
	)
}
