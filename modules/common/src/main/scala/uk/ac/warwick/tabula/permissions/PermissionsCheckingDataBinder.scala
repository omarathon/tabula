package uk.ac.warwick.tabula.permissions
import scala.collection.JavaConversions._
import org.springframework.web.bind.support.WebBindingInitializer
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.InvocableHandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestDataBinderFactory
import javax.servlet.ServletRequest
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.system.BindListener
import org.springframework.util.Assert

class PermissionsCheckingDataBinder(val target: Any, val objectName: String) extends ExtendedServletRequestDataBinder(target, objectName) with Logging {
	
	var securityService: SecurityService = Wire.auto[SecurityService]
	
	def requestInfo = RequestInfo.fromThread
	def user = requestInfo.get.user
		
	// Permissions checking
	if (target.isInstanceOf[PermissionsChecking]) {
		val checkThis = target.asInstanceOf[PermissionsChecking]
		
		Assert.isTrue(!checkThis.permissionsChecks.isEmpty || target.isInstanceOf[Public], "Bind target " + target.getClass + " must specify permissions or extend Public")
		
		for (action <- checkThis.permissionsChecks)
			securityService.check(user, action)
	}

	override def bind(request: ServletRequest) {
		super.bind(request)
		
		// Custom onBind methods
		if (target.isInstanceOf[BindListener])
			target.asInstanceOf[BindListener].onBind
	}
	
}

class PermissionsCheckingDataBinderFactory(binderMethods: List[InvocableHandlerMethod], initializer: WebBindingInitializer) 
	extends ServletRequestDataBinderFactory(binderMethods, initializer) {
	
	override def createBinderInstance(target: Any, objectName: String, request: NativeWebRequest)	= 
		new PermissionsCheckingDataBinder(target, objectName)
	
}