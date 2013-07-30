package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.data._

case class PersonalTutor(student: model.Member) extends BuiltInRole(PersonalTutorRoleDefinition, student)

object PersonalTutorRoleDefinition extends BuiltInRoleDefinition {
	
	override def description = "Personal Tutor"
		
	GrantsScopedPermission(
		Profiles.Read.Core,
		Profiles.Read.NextOfKin,
		Profiles.Read.HomeAddress,
		Profiles.Read.TermTimeAddress,
		Profiles.Read.TelephoneNumber,
		Profiles.Read.MobileNumber,
		Profiles.Read.Usercode,
		Profiles.PersonalTutor.MeetingRecord.Create,
		Profiles.PersonalTutor.MeetingRecord.Read,
		Profiles.PersonalTutor.MeetingRecord.ReadDetails,
		Profiles.PersonalTutor.MeetingRecord.Update,
		Profiles.PersonalTutor.MeetingRecord.Delete,

		SmallGroups.Read
	)
}
