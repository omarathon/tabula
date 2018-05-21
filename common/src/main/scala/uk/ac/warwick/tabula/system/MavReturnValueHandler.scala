package uk.ac.warwick.tabula.system

import scala.collection.JavaConverters._
import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodReturnValueHandler
import org.springframework.web.method.support.ModelAndViewContainer
import uk.ac.warwick.tabula.helpers.{Logging, Ordered}
import uk.ac.warwick.tabula.web.Mav

/**
 * Allows a controller method to return a Mav instead of a ModelAndView.
 */
class MavReturnValueHandler extends HandlerMethodReturnValueHandler with Logging with Ordered {

	override def supportsReturnType(methodParam: MethodParameter): Boolean = {
		classOf[Mav] isAssignableFrom methodParam.getMethod.getReturnType
	}

	override def handleReturnValue(returnValue: Object,
		returnType: MethodParameter,
		mavContainer: ModelAndViewContainer,
		webRequest: NativeWebRequest): Unit =
		returnValue match {
			case mav: Mav =>
				mavContainer.addAllAttributes(mav.toModel.asJava)
				if (mav.viewName != null) {
					mavContainer.setViewName(mav.viewName)
				} else {
					mavContainer.setView(mav.view)
				}
				if (mav.viewName != null && mav.viewName.startsWith("redirect:")
					   && mav.viewName.contains("clearModel=true")) {
					//Don't dump model attributes into the query string when redirecting if we have specified clearModel
					mavContainer.getModel.clear()
				}
				mavContainer.setRequestHandled(false)
		}

}