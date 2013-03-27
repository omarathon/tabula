package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._

import uk.ac.warwick.tabula.permissions.Permissions._

case class UniversityMemberRole(member: model.Member) extends BuiltInRole(member, UniversityMemberRoleDefinition)

case object UniversityMemberRoleDefinition extends BuiltInRoleDefinition {
	GrantsScopedPermission( 
		Profiles.Read.Core,
		Profiles.Read.UniversityId,
		Profiles.Read.NextOfKin,
		Profiles.Read.HomeAddress,
		Profiles.Read.TermTimeAddress,
		Profiles.Read.TelephoneNumber,
		Profiles.Read.MobileNumber,
		Profiles.Read.Usercode,
		Profiles.PersonalTutor.Read,
		Profiles.Read.PersonalTutees,
		Profiles.Read.StudyDetails,
		Profiles.MeetingRecord.Read
	)
	
	GrantsScopelessPermission(
		UserPicker
	)
}