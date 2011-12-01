package uk.ac.warwick.courses.system

import scala.collection.JavaConversions._
import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodReturnValueHandler
import org.springframework.web.method.support.ModelAndViewContainer
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.helpers.Ordered
import uk.ac.warwick.courses.web.Mav

/**
 * Allows a controller method to return a Mav instead of a ModelAndView.
 */
class MavReturnValueHandler extends HandlerMethodReturnValueHandler with Logging with Ordered {

	override def supportsReturnType(methodParam: MethodParameter) = {
		logger.info("Does this suppor the type")
		classOf[Mav] isAssignableFrom methodParam.getMethod.getReturnType
	}

	override def handleReturnValue(returnValue: Object, 
			returnType: MethodParameter, 
			mavContainer: ModelAndViewContainer, 
			webRequest: NativeWebRequest) =
		returnValue match {
			case mav:Mav => {
				mavContainer.addAllAttributes(mav.toModel)
				mavContainer.setViewName(mav.viewName)
				mavContainer.setRequestHandled(false)
			}
		}
		
	

}