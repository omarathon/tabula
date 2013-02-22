package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._

import uk.ac.warwick.tabula.permissions.Permissions._

case class StaffRole(department: model.Department) extends BuiltInRole(department, StaffRoleDefinition)

case object StaffRoleDefinition extends BuiltInRoleDefinition {
	GrantsScopelessPermission(
		UserPicker
	)
	
	GrantsGlobalPermission(
		Profiles.Search
	)
	
	GrantsScopedPermission(
		Profiles.Read,
		Profiles.PersonalTutor.Read
	)
}