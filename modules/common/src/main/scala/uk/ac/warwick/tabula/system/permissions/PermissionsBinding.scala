package uk.ac.warwick.tabula.system.permissions

import scala.collection.JavaConversions._
import org.springframework.web.bind.support.WebBindingInitializer
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.InvocableHandlerMethod
import org.springframework.web.servlet.mvc.method.annotation.ExtendedServletRequestDataBinder
import org.springframework.web.servlet.mvc.method.annotation.ServletRequestDataBinderFactory
import javax.servlet.ServletRequest

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{CurrentUser, ItemNotFoundException, PermissionDeniedException, RequestInfo}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.CustomDataBinder

/**
 * Trait that is added to the data binder to check permissions on the command.
 * Ensures that there some permissions on the command (or that it is set to public).
 *
 * Mixed into the data binder by CustomDataBinderFactory.
 */
trait PermissionsBinding
		extends CustomDataBinder
		with PermissionsCheckingMethods
		with Logging {

	val securityService: SecurityService // abstract dependency

	def requestInfo: Option[RequestInfo] = RequestInfo.fromThread
	def user: CurrentUser = requestInfo.get.user

	// Permissions checking
	if (target.isInstanceOf[PermissionsChecking]) {
		permittedByChecks(securityService, user, target.asInstanceOf[PermissionsChecking])
	}
}
