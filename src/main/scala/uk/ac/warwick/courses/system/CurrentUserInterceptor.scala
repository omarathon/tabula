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
import uk.ac.warwick.courses.web.Cookies._
import uk.ac.warwick.userlookup.UserLookup 

class CurrentUserInterceptor extends HandlerInterceptorAdapter {
    @Autowired var securityService:SecurityService =_
    @Autowired var userLookup:UserLookup =_
  
	override def preHandle(request:HttpServletRequest, response:HttpServletResponse, obj:Any) = {
	  val currentUser:CurrentUser = request.getAttribute("SSO_USER") match {
	    case user:User if user.isFoundUser => {
	    	val sysadmin = isSysadmin(user)
	    	new CurrentUser(user, sysadmin, apparentUser(request, user, sysadmin))
	    }
	    case _ => NoCurrentUser()
	  }
	  request.setAttribute(CurrentUser.keyName, currentUser)
	  true //allow request to continue
	}
    
    private def isSysadmin(user:User) = securityService.isSysadmin(user.getUserId()) 
    
    // masquerade support
    private def apparentUser(request:HttpServletRequest, realUser:User, sysadmin:Boolean):User = 
    	if (sysadmin) {
    		request.getCookies.getString("coursesMasqueradeAs") match {
    			case Some(userid) => userLookup.getUserByUserId(userid) match {
    				case user:User if user.isFoundUser() => user
    				case _ => realUser
    			}
    			case None => realUser
    		}
    	} else {
    		realUser
    	}
    
}