package uk.ac.warwick.tabula.system

import org.springframework.web.method.support.HandlerMethodReturnValueHandler
import collection.JavaConverters._
import collection.JavaConversions._
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite
import org.springframework.web.method.support.InvocableHandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestDataBinderFactory

/**
 * Extension of RequestMappingHandlerAdapter that allows you to place
 * custom HandlerMethodReturnValueHandlers _before_ the default ones, without having
 * to replace them entirely.
 */
class HandlerAdapter extends org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter {
	var customPreReturnValueHandlers: JList[HandlerMethodReturnValueHandler] = Nil

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
	
	override def createDataBinderFactory(binderMethods: JList[InvocableHandlerMethod]): ServletRequestDataBinderFactory = 
		new CustomDataBinderFactory(binderMethods.toList, getWebBindingInitializer())

}
