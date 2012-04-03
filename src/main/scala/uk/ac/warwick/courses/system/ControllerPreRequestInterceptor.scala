package uk.ac.warwick.courses.system

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.springframework.web.method.HandlerMethod
import uk.ac.warwick.courses.web.controllers.PreRequestHandler

class ControllerPreRequestInterceptor extends HandlerInterceptorAdapter {
	override def preHandle(request:HttpServletRequest, response:HttpServletResponse, obj:Any) = {
		obj match {
			case method:HandlerMethod => method.getBean match {
				case controller:PreRequestHandler => controller.preRequest
				case _ =>
			}
			case _ =>
		}
		true
	}
}