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
import uk.ac.warwick.util.web.Uri

class RequestInfoInterceptor extends HandlerInterceptorAdapter {
  
	val AjaxHeader = "X-Requested-With"
	val XRequestedUriHeader = "X-Requested-Uri"
	
	override def preHandle(request:HttpServletRequest, response:HttpServletResponse, obj:Any) = {
		RequestInfo.open(new RequestInfo(
				user = getUser(request),
				requestedUri = getRequestedUri(request),
				ajax = isAjax(request)
		))
		true
	}
	
	private def getUser(request:HttpServletRequest) = request.getAttribute(CurrentUser.keyName) match {
		 	case user:CurrentUser => user
		 	case _ => null
		}
	
	private def isAjax(request:HttpServletRequest) = request.getHeader(AjaxHeader) match {
			case "XMLHttpRequest" => true
			case _ => false
		}
	
	private def getRequestedUri(request:HttpServletRequest) = Uri.parse(request.getHeader(XRequestedUriHeader) match {
			case string:String => string
			case _ => request.getRequestURL.toString
		})
	
//	
//	
//	override def afterCompletion(request:HttpServletRequest , response:HttpServletResponse , handler:Object, ex:Exception) {
//		RequestInfo.close
//	}
    
}