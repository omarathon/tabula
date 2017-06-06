package uk.ac.warwick.tabula.system
import org.springframework.beans.factory.InitializingBean
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest

import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletResponse

import org.springframework.web.servlet.support.RequestContext
import org.springframework.web.context.request.WebRequestInterceptor
import org.springframework.web.context.request.WebRequest
import org.springframework.ui.ModelMap
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter

/**
 * Wraps a WebRequestInterceptor, and if it matches an exclude pattern, it doesn't
 * run the interceptor.
 */
class ConditionalInterceptor(val delegate: WebRequestInterceptor) extends WebRequestInterceptor with InitializingBean {
	var excludePath: String = _

	private var excludeString: String = _

	override def afterPropertiesSet {
		if (!excludePath.endsWith("/*")) throw new IllegalArgumentException("excludePath only knows how to end in /*")
		excludeString = excludePath.substring(0, excludePath.length - 2)
	}

	private def req(request: WebRequest) = request.asInstanceOf[ServletWebRequest].getRequest
	private def path(request: HttpServletRequest): String = request.getRequestURI.substring(request.getContextPath.length)
	private def path(request: WebRequest): String = path(req(request))

	private def included(request: WebRequest) = !(path(request).startsWith(excludeString))

	override def preHandle(request: WebRequest): Unit =
		if (included(request)) delegate.preHandle(request)

	override def postHandle(request: WebRequest, model: ModelMap): Unit =
		if (included(request)) delegate.postHandle(request, model)

	override def afterCompletion(request: WebRequest, ex: Exception): Unit =
		if (included(request)) delegate.afterCompletion(request, ex)

}

/**
	* Wraps a HandlerInterceptorAdapter, and if it matches an exclude pattern, it doesn't run the interceptor.
	*/
class ConditionalHandlerInterceptorAdapter(val delegate: HandlerInterceptorAdapter) extends HandlerInterceptorAdapter with InitializingBean {
	var excludePath: String = _

	private var excludeString: String = _

	override def afterPropertiesSet() {
		if (!excludePath.endsWith("/*")) throw new IllegalArgumentException("excludePath only knows how to end in /*")
		excludeString = excludePath.substring(0, excludePath.length - 2)
	}

	private def path(request: HttpServletRequest): String = request.getRequestURI.substring(request.getContextPath.length)
	private def included(request: HttpServletRequest) = !path(request).startsWith(excludeString)

	override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean =
		if (included(request)) delegate.preHandle(request, response, handler) else true

	override def postHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any, modelAndView: ModelAndView): Unit =
		if (included(request)) delegate.postHandle(request, response, handler, modelAndView)

	override def afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception): Unit =
		if (included(request)) delegate.afterCompletion(request, response, handler, ex)

	override def afterConcurrentHandlingStarted(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Unit =
		if (included(request)) delegate.afterConcurrentHandlingStarted(request, response, handler)

}