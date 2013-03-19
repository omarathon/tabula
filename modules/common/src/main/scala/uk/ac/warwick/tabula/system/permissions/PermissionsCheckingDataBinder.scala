package uk.ac.warwick.tabula.system.permissions

import scala.collection.JavaConversions._
import org.springframework.util.Assert
import org.springframework.web.bind.support.WebBindingInitializer
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.InvocableHandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestDataBinderFactory
import javax.servlet.ServletRequest
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.ItemNotFoundException

class PermissionsCheckingDataBinder(val target: Any, val objectName: String, val securityService: SecurityService) 
	extends ExtendedServletRequestDataBinder(target, objectName) 
		with Logging {
	
	// Autowired third parameter. Done in a skewiff way for testing purposes
	def this(target: Any, objectName: String) = {
		this(target, objectName, Wire.auto[SecurityService])
	}
	
	def requestInfo = RequestInfo.fromThread
	def user = requestInfo.get.user
		
	// Permissions checking
	if (target.isInstanceOf[PermissionsChecking]) {
		val checkThis = target.asInstanceOf[PermissionsChecking]
		
		Assert.isTrue(
			!checkThis.permissionChecks.isEmpty || target.isInstanceOf[Public],
			"Bind target " + target.getClass + " must specify permissions or extend Public"
		)
		
		for (check <- checkThis.permissionChecks) check match {
			case (permission: Permission, Some(scope)) => securityService.check(user, permission, scope)
			case (permission: ScopelessPermission, _) => securityService.check(user, permission)
			case _ =>
				// We're trying to do a permissions check against a non-existent scope - 404
				logger.warn("Permissions check throwing item not found - this should be caught in command (" + target + ")")
				throw new ItemNotFoundException()
		}
	}

	override def bind(request: ServletRequest) {
		super.bind(request)
		
		// Custom onBind methods
		if (target.isInstanceOf[BindListener])
			target.asInstanceOf[BindListener].onBind(super.getBindingResult)
	}
	
}

class PermissionsCheckingDataBinderFactory(binderMethods: List[InvocableHandlerMethod], initializer: WebBindingInitializer) 
	extends ServletRequestDataBinderFactory(binderMethods, initializer) {
	
	override def createBinderInstance(target: Any, objectName: String, request: NativeWebRequest)	= 
		new PermissionsCheckingDataBinder(target, objectName)
	
}