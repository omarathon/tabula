package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._

case class Masquerader extends BuiltInRole(None) {
	
	GrantsPermission(
		Masquerade
	)
	
}