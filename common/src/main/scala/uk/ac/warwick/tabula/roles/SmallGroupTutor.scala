package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent}
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsSelector

case class SmallGroupEventTutor(smallGroupEvent: SmallGroupEvent) extends BuiltInRole(SmallGroupEventTutorRoleDefinition, smallGroupEvent)
case class SmallGroupTutor(smallGroup: SmallGroup) extends BuiltInRole(SmallGroupTutorRoleDefinition, smallGroup)
case class SmallGroupMembersTutor(student: StudentMember) extends BuiltInRole(SmallGroupMembersTutorRoleDefinition, student)

case object SmallGroupEventTutorRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Small group event tutor"

	GrantsScopedPermission(
		SmallGroupEvents.Register
	)

}

case object SmallGroupTutorRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Small group tutor"

	GrantsScopedPermission(
		SmallGroups.Read,
		SmallGroups.ReadMembership,
		SmallGroupEvents.ViewRegister
	)

}

case object SmallGroupMembersTutorRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Small group member's tutor"

	GrantsScopedPermission(
		// Extra permissions on any students in these groups for their profile
		Profiles.Read.Usercode,
		Profiles.Read.StudentCourseDetails.Core,
		Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]),
		Profiles.Read.StudentCourseDetails.Status,
		Profiles.Read.Timetable,
		Profiles.Read.SmallGroups,
		Profiles.Read.Disability, // TAB-4386
		Profiles.Read.Photo
	)

}