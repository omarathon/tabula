package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._

import uk.ac.warwick.tabula.permissions.Permissions._

case class UniversityMemberRole(member: model.Member) extends BuiltInRole(member, UniversityMemberRoleDefinition)

case object UniversityMemberRoleDefinition extends BuiltInRoleDefinition {
	// As per discussion in TAB-753, anyone at the University can see anyone else's core information
	GrantsGlobalPermission( 
		Profiles.Read.Core
	)
	
	GrantsScopedPermission(
		Profiles.Read.NextOfKin,
		Profiles.Read.HomeAddress,
		Profiles.Read.TermTimeAddress,
		Profiles.Read.TelephoneNumber,
		Profiles.Read.MobileNumber,
		Profiles.Read.Usercode,
		Profiles.PersonalTutor.Read,
		Profiles.Read.PersonalTutees,
		Profiles.Read.StudyDetails,

		Profiles.MeetingRecord.Read,
		Profiles.MeetingRecord.ReadDetails,
		Profiles.MeetingRecord.Create,
		Profiles.MeetingRecord.Update,
		Profiles.MeetingRecord.Delete
	)
	
	GrantsScopelessPermission(
		UserPicker
	)
}