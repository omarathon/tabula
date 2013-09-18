package uk.ac.warwick.tabula.system

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.sso.client.SSOClientFilter
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.services.SecurityService
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.util.web.Uri
import collection.JavaConversions._
import collection.JavaConverters._
import uk.ac.warwick.tabula.services.MaintenanceModeService
import uk.ac.warwick.tabula.helpers.RequestLevelCache

/** Provides a limited interface of request-level things, which are required by some objects
	* like CurrentUser before a full RequestInfo can be created.
	*/
class EarlyRequestInfoInterceptor extends HandlerInterceptorAdapter {

	override def preHandle(request: HttpServletRequest, response: HttpServletResponse, obj: Any) = {
		implicit val req = request
		EarlyRequestInfo.open(new EarlyRequestInfoImpl())
		true
	}

	override def afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Object, ex: Exception) {
		EarlyRequestInfo.close()
	}
}

class RequestInfoInterceptor extends HandlerInterceptorAdapter {
	import RequestInfoInterceptor._
  
	@Autowired var maintenance: MaintenanceModeService = _

	override def preHandle(request: HttpServletRequest, response: HttpServletResponse, obj: Any) = {
		implicit val req = request
		RequestInfo.open(fromAttributeElse(newRequestInfo(request, maintenance.enabled)))
		true
	}

	/**
	 * Gets a RequestInfo from a request attribute, else constructs
	 * it from the given code block and stores it in an attribute.
	 * Stored in an attribute so that forwarded requests use the same
	 * object.
	 */
	private def fromAttributeElse(ifEmpty: => RequestInfo)(implicit request: HttpServletRequest): RequestInfo = {
		Option(request.getAttribute(RequestInfoAttribute).asInstanceOf[RequestInfo]).getOrElse {
			val info = ifEmpty
			request.setAttribute(RequestInfoAttribute, info)
			info
		}
	}

	override def afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Object, ex: Exception) {
		RequestInfo.close
	}

}

object RequestInfoInterceptor {
	val AjaxHeader = "X-Requested-With"
	val XRequestedUriHeader = "X-Requested-Uri"
	val RequestInfoAttribute = "APP_REQUEST_INFO_ATTRIBUTE"
	  
	def newRequestInfo(request: HttpServletRequest, isMaintenance: Boolean = false) = {
		// Transfer cache from an EarlyAccessInfo if one exists.
		val cache = EarlyRequestInfo.fromThread map { _.requestLevelCache } getOrElse { new RequestLevelCache() }

		new RequestInfo(
			user = getUser(request),
			requestedUri = getRequestedUri(request),
			requestParameters = getParameters(request),
			ajax = isAjax(request),
			maintenance = isMaintenance,
			requestLevelCache = cache)
	}

	private def getUser(implicit request: HttpServletRequest) = request.getAttribute(CurrentUser.keyName) match {
		case user: CurrentUser => user
		case _ => null
	}

	private def isAjax(implicit request: HttpServletRequest) = hasXHRHeader || hasAJAXParam

	private def hasXHRHeader(implicit request: HttpServletRequest) = request.getHeader(AjaxHeader) match {
		case "XMLHttpRequest" => true
		case _ => false
	}

	private def hasAJAXParam(implicit request: HttpServletRequest) = request.getParameter("ajax") match {
		case s: String => true
		case _ => false
	}

	private def getRequestedUri(request: HttpServletRequest) = Uri.parse(request.getHeader(XRequestedUriHeader) match {
		case string: String => string
		case _ => request.getRequestURL.toString
	})
	
	private def getParameters(request: HttpServletRequest) = {
		val params: Map[String, List[String]] = 
			for ((key, value) <- request.getParameterMap().toMap)
				yield (key, value) match {
					case (key: String, values: Array[String]) => (key, values.toList)
				}
				
		params.toMap
	}
}