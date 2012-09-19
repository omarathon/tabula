package uk.ac.warwick.courses.system
import org.springframework.web.method.support.HandlerMethodReturnValueHandler
import scala.reflect.BeanProperty
import collection.JavaConversions._
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import uk.ac.warwick.courses.JavaImports._
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite

/**
 * Extension of RequestMappingHandlerAdapter that allows you to place
 * custom HandlerMethodReturnValueHandlers _before_ the default ones, without having
 * to replace them entirely.
 */
class HandlerAdapter extends org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter {
	@BeanProperty var customPreReturnValueHandlers: java.util.List[HandlerMethodReturnValueHandler] = Nil

	/*
	 * There used to be a protected method we could override but now it's all private, so
	 * we use reflection to modify the private field on startup.
	 */
	private val returnValueHandlersField = {
		val field = classOf[RequestMappingHandlerAdapter].getDeclaredField("returnValueHandlers")
		field.setAccessible(true)
		field
	}

	override def afterPropertiesSet = {
		super.afterPropertiesSet()
		val defaultHandlers = returnValueHandlersField.get(this).asInstanceOf[HandlerMethodReturnValueHandlerComposite]
		val composite = new HandlerMethodReturnValueHandlerComposite
		composite.addHandlers(customPreReturnValueHandlers)
		composite.addHandler(defaultHandlers)
		returnValueHandlersField.set(this, composite)
	}

}
