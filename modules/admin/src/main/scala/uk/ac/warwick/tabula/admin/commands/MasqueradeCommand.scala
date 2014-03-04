package uk.ac.warwick.tabula.admin.commands

import uk.ac.warwick.tabula.web.Cookie
import uk.ac.warwick.tabula.commands.{CommandInternal, Describable, ComposableCommand, ReadOnly, Description}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.helpers.NoUser
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}

object MasqueradeCommand {
	def apply() =
		new MasqueradeCommandInternal
				with ComposableCommand[Option[Cookie]]
				with MasqueradeCommandPermissions
				with MasqueradeCommandDescription
				with ReadOnly
				with AutowiringUserLookupComponent
}

class MasqueradeCommandInternal extends CommandInternal[Option[Cookie]] with MasqueradeCommandState {
	self: UserLookupComponent =>

	def applyInternal() = {
		if (action == "remove") Some(newCookie(null))
		else userLookup.getUserByUserId(usercode) match {
			case FoundUser(user) => Some(newCookie(usercode))
			case NoUser(user) => None
		}
	}

	private def newCookie(usercode: String) = new Cookie(
		name = CurrentUser.masqueradeCookie,
		value = usercode,
		path = "/")

}

trait MasqueradeCommandState {
	var usercode: String = _
	var action: String = _
}

trait MasqueradeCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Masquerade)
	}
}

trait MasqueradeCommandDescription extends Describable[Option[Cookie]] {
	self: MasqueradeCommandState =>

	def describe(d: Description) = d.properties(
		"usercode" -> usercode,
		"action" -> action
	)
}