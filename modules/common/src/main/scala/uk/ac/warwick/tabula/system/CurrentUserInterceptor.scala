package uk.ac.warwick.tabula.system

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.sso.client.SSOClientFilter
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.NoCurrentUser
import uk.ac.warwick.tabula.services.SecurityService
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.web.Cookies._
import uk.ac.warwick.userlookup.UserLookup
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.tabula.helpers.FoundUser

class CurrentUserInterceptor extends HandlerInterceptorAdapter {
	@Autowired var securityService: SecurityService = _
	@Autowired var userLookup: UserLookupInterface = _

	type MasqueradeUserCheck = (User, Boolean) => User

	def resolveCurrentUser(user: User, masqueradeUser: MasqueradeUserCheck, godModeEnabled: => Boolean) = {
		val sysadmin = securityService.isSysadmin(user.getUserId())
		val god = sysadmin && godModeEnabled
		val masquerader = securityService.isMasquerader(user.getUserId)
		new CurrentUser(
			realUser = user,
			apparentUser = masqueradeUser(user, sysadmin),
			sysadmin = sysadmin,
			masquerader = masquerader,
			god = god)
	}

	override def preHandle(request: HttpServletRequest, response: HttpServletResponse, obj: Any) = {
		val currentUser: CurrentUser = request.getAttribute(SSOClientFilter.USER_KEY) match {
			case FoundUser(user) => resolveCurrentUser(user, apparentUser(request), godCookieExists(request))
			case _ => NoCurrentUser()
		}
		request.setAttribute(CurrentUser.keyName, currentUser)
		true //allow request to continue
	}

	private def godCookieExists(request: HttpServletRequest): Boolean =
		request.getCookies().getBoolean(CurrentUser.godModeCookie, false)

	// masquerade support
	private def apparentUser(request: HttpServletRequest)(realUser: User, sysadmin: Boolean): User =
		if (sysadmin) {
			request.getCookies.getString(CurrentUser.masqueradeCookie) match {
				case Some(userid) => userLookup.getUserByUserId(userid) match {
					case user: User if user.isFoundUser() => user
					case _ => realUser
				}
				case None => realUser
			}
		} else {
			realUser
		}

}