package uk.ac.warwick.tabula.system
import scala.collection.JavaConversions._
import org.springframework.web.bind.support.WebBindingInitializer
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.InvocableHandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestDataBinderFactory
import uk.ac.warwick.tabula.system.permissions.PermissionsBinding
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SecurityService

/**
 * Factory that creates a DataBinder instance every time an object needs binding
 * from a request. We use our own custom data binder. 
 */
class CustomDataBinderFactory(binderMethods: List[InvocableHandlerMethod], initializer: WebBindingInitializer) 
	extends ServletRequestDataBinderFactory(binderMethods, initializer) {
	
	trait CustomDataBinderDependencies {
		// dependency for PermissionsBinding
		val securityService = Wire.auto[SecurityService]
	}
	
	trait NoAutoGrownNestedPaths extends CustomDataBinder {
		setAutoGrowNestedPaths(false)
	}
	
	override def createBinderInstance(target: Any, objectName: String, request: NativeWebRequest)	= { 
		new CustomDataBinder(target, objectName) 
				with CustomDataBinderDependencies
				with PermissionsBinding
				with AllowedFieldsBinding
				with BindListenerBinding
				with NoAutoGrownNestedPaths
	}
	
}