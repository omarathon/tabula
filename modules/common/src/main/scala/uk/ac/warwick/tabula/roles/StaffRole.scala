package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._

import uk.ac.warwick.tabula.permissions.Permissions._

case class StaffRole(department: model.Department) extends BuiltInRole(department) {
	GrantsPermission(
		UserPicker,
		UserSettings.Update
	)
	
	GrantsGlobalPermission(
		Profiles.Search
	)
	
	GrantsPermissionFor(department,
		Profiles.Read,
		Profiles.PersonalTutor.Read
	)
}