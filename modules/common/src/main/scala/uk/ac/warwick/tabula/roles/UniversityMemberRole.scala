package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._

import uk.ac.warwick.tabula.permissions.Permissions._

case class UniversityMemberRole(member: model.Member) extends BuiltInRole(member) {
	GrantsPermissionFor(member, 
		Profiles.Read,
		Profiles.PersonalTutor.Read
	)
	
	GrantsPermission(
		UserPicker
	)
}