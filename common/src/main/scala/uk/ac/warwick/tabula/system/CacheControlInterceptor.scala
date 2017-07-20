package uk.ac.warwick.tabula.system

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter

class CacheControlInterceptor extends HandlerInterceptorAdapter {

	override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean = {
		response.setHeader("Cache-control", "no-cache, no-store")
		response.setHeader("Pragma", "no-cache")
		response.setHeader("Expires", "0")
		true //allow request to continue
	}
}
