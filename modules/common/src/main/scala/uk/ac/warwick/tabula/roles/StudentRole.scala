package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._

import uk.ac.warwick.tabula.permissions.Permissions._

case class StudentRole(department: model.Department) extends BuiltInRole {
	GrantsRole(UniversityMemberRole(department))
}