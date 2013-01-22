package uk.ac.warwick.tabula.home.commands.admin

import uk.ac.warwick.tabula.web.Cookie
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.ReadOnly
import org.hibernate.validator.constraints.NotEmpty
import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.actions.Masquerade
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.NoUser
import uk.ac.warwick.tabula.helpers.FoundUser
import uk.ac.warwick.tabula.commands.Description

class MasqueradeCommand extends Command[Option[Cookie]] with ReadOnly {
	
	PermissionsCheck(Masquerade())
	
	var userLookup = Wire.auto[UserLookupInterface]
	
	@BeanProperty var usercode: String = _	
	@BeanProperty var action: String = _
	
	def applyInternal() = {
		if (action == "remove") Some(newCookie(null))
		else userLookup.getUserByUserId(usercode) match {
			case FoundUser(user) => Some(newCookie(usercode))
			case NoUser(user) => None
		}
	}

	private def newCookie(usercode: String) = new Cookie(
		name = "tabulaMasqueradeAs",
		value = usercode,
		path = "/")
	
	def describe(d: Description) = d.properties(
		"usercode" -> usercode, 
		"action" -> action
	)

}