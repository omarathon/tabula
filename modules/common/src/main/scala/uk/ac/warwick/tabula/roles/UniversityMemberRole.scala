package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._

import uk.ac.warwick.tabula.permissions.Permissions._

case class UniversityMemberRole(member: model.Member) extends BuiltInRole(member, UniversityMemberRoleDefinition)

case object UniversityMemberRoleDefinition extends BuiltInRoleDefinition {
	GrantsScopedPermission( 
		Profiles.Read,
		Profiles.PersonalTutor.Read
	)
	
	GrantsScopelessPermission(
		UserPicker
	)
}