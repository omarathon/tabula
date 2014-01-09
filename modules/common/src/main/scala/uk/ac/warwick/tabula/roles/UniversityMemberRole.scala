package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

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
		Profiles.Read.StudentCourseDetails.Core,
		Profiles.Read.StudentCourseDetails.Status,
		Profiles.Read.SmallGroups,
		Profiles.Read.Coursework,
    Profiles.Read.Timetable,

    Profiles.Read.RelationshipStudents(PermissionsSelector.Any[StudentRelationshipType]),
		
    Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]),

    Profiles.MeetingRecord.Read(PermissionsSelector.Any[StudentRelationshipType]),
    Profiles.MeetingRecord.ReadDetails(PermissionsSelector.Any[StudentRelationshipType]),
    Profiles.MeetingRecord.Create(PermissionsSelector.Any[StudentRelationshipType]),
    Profiles.MeetingRecord.Update(PermissionsSelector.Any[StudentRelationshipType]),
    Profiles.MeetingRecord.Delete(PermissionsSelector.Any[StudentRelationshipType]),

		MonitoringPoints.View,
		SmallGroupEvents.ViewRegister,
		
		// Can read own coursework info
		Submission.Read,
		Feedback.Read,
		Extension.Read
	)

	GrantsScopelessPermission(
		UserPicker
	)
}
