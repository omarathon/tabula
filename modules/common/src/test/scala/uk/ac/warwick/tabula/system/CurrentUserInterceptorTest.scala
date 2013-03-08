package uk.ac.warwick.tabula.system
import org.mockito.Matchers._
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

import javax.servlet.http
import uk.ac.warwick.sso.client.SSOClientFilter
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.roles.Masquerader
import uk.ac.warwick.tabula.roles.Sysadmin
import uk.ac.warwick.tabula.services.permissions.RoleService
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.userlookup.User

class CurrentUserInterceptorTest extends TestBase with Mockito {
	
	val interceptor = new CurrentUserInterceptor
	
	val roleService = mock[RoleService]
	val userLookup = new MockUserLookup
	
	interceptor.roleService = roleService
	interceptor.userLookup = userLookup
	
	@Test def foundUser {
		val user = new User("cuscav")
		user.setFoundUser(true)
		
		val req = new MockHttpServletRequest
		req.setAttribute(SSOClientFilter.USER_KEY, user)
		
		val resp = new MockHttpServletResponse
		
		interceptor.preHandle(req, resp, null) should be (true)
		
		val currentUser = req.getAttribute(CurrentUser.keyName).asInstanceOf[CurrentUser]
		currentUser should not be (null)
		
		currentUser.realUser should be (user)
		currentUser.apparentUser should be (user)
		currentUser.god should be (false)
		currentUser.masquerader should be (false)
		currentUser.sysadmin should be (false)
	}
	
	@Test def notFoundUser {
		val user = new User("cuscav")
		user.setFoundUser(false)
		
		val req = new MockHttpServletRequest
		req.setAttribute(SSOClientFilter.USER_KEY, user)
		
		val resp = new MockHttpServletResponse
		
		interceptor.preHandle(req, resp, null) should be (true)
		
		val currentUser = req.getAttribute(CurrentUser.keyName).asInstanceOf[CurrentUser]
		currentUser should not be (null)
		
		currentUser.realUser.isInstanceOf[AnonymousUser] should be (true)
		currentUser.apparentUser.isInstanceOf[AnonymousUser] should be (true)
		currentUser.god should be (false)
		currentUser.masquerader should be (false)
		currentUser.sysadmin should be (false)
	}
	
	@Test def masquerading {
		val user = new User("cuscav")
		user.setFoundUser(true)
		
		val masque = new User("cusebr")
		masque.setFoundUser(true)
		
		roleService.hasRole(isA[CurrentUser], isEq(Masquerader())) returns (true)
		userLookup.users += ("cusebr" -> masque)
		
		val req = new MockHttpServletRequest
		req.setAttribute(SSOClientFilter.USER_KEY, user)
		req.setCookies(new http.Cookie(CurrentUser.masqueradeCookie, "cusebr"))
		
		val resp = new MockHttpServletResponse
		
		interceptor.preHandle(req, resp, null) should be (true)
		
		val currentUser = req.getAttribute(CurrentUser.keyName).asInstanceOf[CurrentUser]
		currentUser should not be (null)
		
		currentUser.realUser should be (user)
		currentUser.apparentUser should be (masque)
		currentUser.god should be (false)
		currentUser.masquerader should be (true)
		currentUser.sysadmin should be (false)
	}
	
	@Test def masqueradeAttemptButNotMasquerader {
		val user = new User("cuscav")
		user.setFoundUser(true)
		
		val masque = new User("cusebr")
		masque.setFoundUser(true)
		
		roleService.hasRole(isA[CurrentUser], isEq(Masquerader())) returns (false)
		userLookup.users += ("cusebr" -> masque)
		
		val req = new MockHttpServletRequest
		req.setAttribute(SSOClientFilter.USER_KEY, user)
		req.setCookies(new http.Cookie(CurrentUser.masqueradeCookie, "cusebr"))
		
		val resp = new MockHttpServletResponse
		
		interceptor.preHandle(req, resp, null) should be (true)
		
		val currentUser = req.getAttribute(CurrentUser.keyName).asInstanceOf[CurrentUser]
		currentUser should not be (null)
		
		currentUser.realUser should be (user)
		currentUser.apparentUser should be (user)
		currentUser.god should be (false)
		currentUser.masquerader should be (false)
		currentUser.sysadmin should be (false)
	}
	
	@Test def sysadmin {
		val user = new User("cuscav")
		user.setFoundUser(true)
		
		roleService.hasRole(isA[CurrentUser], isEq(Sysadmin())) returns (true)
		
		val req = new MockHttpServletRequest
		req.setAttribute(SSOClientFilter.USER_KEY, user)
		
		val resp = new MockHttpServletResponse
		
		interceptor.preHandle(req, resp, null) should be (true)
		
		val currentUser = req.getAttribute(CurrentUser.keyName).asInstanceOf[CurrentUser]
		currentUser should not be (null)
		
		currentUser.realUser should be (user)
		currentUser.apparentUser should be (user)
		currentUser.god should be (false)
		currentUser.masquerader should be (false)
		currentUser.sysadmin should be (true)
	}
	
	@Test def god {
		val user = new User("cuscav")
		user.setFoundUser(true)
		
		roleService.hasRole(isA[CurrentUser], isEq(Sysadmin())) returns (true)
		
		val req = new MockHttpServletRequest
		req.setAttribute(SSOClientFilter.USER_KEY, user)
		req.setCookies(new http.Cookie(CurrentUser.godModeCookie, "true"))
		
		val resp = new MockHttpServletResponse
		
		interceptor.preHandle(req, resp, null) should be (true)
		
		val currentUser = req.getAttribute(CurrentUser.keyName).asInstanceOf[CurrentUser]
		currentUser should not be (null)
		
		currentUser.realUser should be (user)
		currentUser.apparentUser should be (user)
		currentUser.god should be (true)
		currentUser.masquerader should be (false)
		currentUser.sysadmin should be (true)
	}
	
	@Test def godNotSysadmin {
		val user = new User("cuscav")
		user.setFoundUser(true)
		
		roleService.hasRole(isA[CurrentUser], isEq(Sysadmin())) returns (false)
		
		val req = new MockHttpServletRequest
		req.setAttribute(SSOClientFilter.USER_KEY, user)
		req.setCookies(new http.Cookie(CurrentUser.godModeCookie, "true"))
		
		val resp = new MockHttpServletResponse
		
		interceptor.preHandle(req, resp, null) should be (true)
		
		val currentUser = req.getAttribute(CurrentUser.keyName).asInstanceOf[CurrentUser]
		currentUser should not be (null)
		
		currentUser.realUser should be (user)
		currentUser.apparentUser should be (user)
		currentUser.god should be (false)
		currentUser.masquerader should be (false)
		currentUser.sysadmin should be (false)
	}

}