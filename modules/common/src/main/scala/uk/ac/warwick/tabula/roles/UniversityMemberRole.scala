package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._

import uk.ac.warwick.tabula.permissions.Permissions._

case class UniversityMemberRole(member: model.Member) extends BuiltInRole(UniversityMemberRoleDefinition, member)

case object UniversityMemberRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "University Member"

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
		Profiles.Supervisor.Read,
		Profiles.Read.PersonalTutees,
		Profiles.Read.StudentCourseDetails.Core,
		Profiles.Read.StudentCourseDetails.Status,
		Profiles.Read.Supervisees,
		Profiles.Read.SmallGroups,

		Profiles.PersonalTutor.MeetingRecord.Read,
    Profiles.PersonalTutor.MeetingRecord.ReadDetails,
    Profiles.PersonalTutor.MeetingRecord.Create,
    Profiles.PersonalTutor.MeetingRecord.Update,
    Profiles.PersonalTutor.MeetingRecord.Delete,
    Profiles.Supervisor.MeetingRecord.Read,
    Profiles.Supervisor.MeetingRecord.ReadDetails,
    Profiles.Supervisor.MeetingRecord.Create,
    Profiles.Supervisor.MeetingRecord.Update,
    Profiles.Supervisor.MeetingRecord.Delete
	)

	GrantsScopelessPermission(
		UserPicker
	)
}