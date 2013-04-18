package uk.ac.warwick.tabula.system

import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SecurityService

/**
 * The base of our custom data binder. This is made concrete in CustomDataBinderFactory where we
 * mix in the traits that define the actual behaviour.
 */
abstract class CustomDataBinder(val target: Any, val objectName: String)
		extends ExtendedServletRequestDataBinder(target, objectName) {
	
	// getPropertyAccessor is protected, this lets us access it from a trait.
	def propertyAccessor = getPropertyAccessor()
	
}