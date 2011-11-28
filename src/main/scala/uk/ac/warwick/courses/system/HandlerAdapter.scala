package uk.ac.warwick.courses.system
import org.springframework.web.method.support.HandlerMethodReturnValueHandler
import scala.reflect.BeanProperty
import collection.JavaConversions._

/**
 * Extension of RequestMappingHandlerAdapter that allows you to place
 * custom HandlerMethodReturnValueHandlers _before_ the default ones, without having
 * to replace them entirely.
 */
class HandlerAdapter extends org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter {
	@BeanProperty var customPreReturnValueHandlers:java.util.List[HandlerMethodReturnValueHandler] = Nil
	
	override def getDefaultReturnValueHandlers = 
		customPreReturnValueHandlers ++ super.getDefaultReturnValueHandlers
		
		
}
