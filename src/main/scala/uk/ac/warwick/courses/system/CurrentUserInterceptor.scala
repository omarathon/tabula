package uk.ac.warwick.courses.system

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.sso.client.SSOClientFilter
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.NoCurrentUser
import uk.ac.warwick.courses.services.SecurityService
import org.springframework.beans.factory.annotation.Autowired

class CurrentUserInterceptor extends HandlerInterceptorAdapter {
    @Autowired var securityService:SecurityService =_
  
	override def preHandle(request:HttpServletRequest, response:HttpServletResponse, obj:Any) = {
	  val currentUser:CurrentUser = SSOClientFilter.getUserFromRequest(request) match {
	    case user:User if user.isFoundUser => new CurrentUser(user, isSysadmin(user))
	    case _ => NoCurrentUser()
	  }
	  request.setAttribute(CurrentUser.keyName, currentUser)
	  true //allow request to continue
	}
    
    private def isSysadmin(user:User) = securityService.isSysadmin(user.getUserId()) 
}