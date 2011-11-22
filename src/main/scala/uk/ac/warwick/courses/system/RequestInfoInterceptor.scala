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
import uk.ac.warwick.courses.RequestInfo

class RequestInfoInterceptor extends HandlerInterceptorAdapter {
  
	override def preHandle(request:HttpServletRequest, response:HttpServletResponse, obj:Any) = {
		request.getAttribute(CurrentUser.keyName) match {
		 	case user:CurrentUser => RequestInfo.open(new RequestInfo(user))
		}
		true
	}
	
//	
//	
//	override def afterCompletion(request:HttpServletRequest , response:HttpServletResponse , handler:Object, ex:Exception) {
//		RequestInfo.close
//	}
    
}