package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.data._

case class PersonalTutor(student: model.Member) extends BuiltInRole(student, PersonalTutorRoleDefinition)

object PersonalTutorRoleDefinition extends BuiltInRoleDefinition {
	GrantsScopedPermission(
		Profiles.Read
	)
}