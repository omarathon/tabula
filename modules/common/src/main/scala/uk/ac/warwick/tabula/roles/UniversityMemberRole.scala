package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._

import uk.ac.warwick.tabula.permissions.Permissions._

case class UniversityMemberRole(department: model.Department) extends BuiltInRole(department) {
	GrantsPermission(
		UserPicker
	)
}