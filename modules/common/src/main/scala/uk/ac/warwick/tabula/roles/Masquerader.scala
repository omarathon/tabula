package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._

case class Masquerader extends BuiltInRole(None, MasqueraderRoleDefinition)

case object MasqueraderRoleDefinition extends BuiltInRoleDefinition {
	
	GrantsScopelessPermission(
		Masquerade
	)
	
}