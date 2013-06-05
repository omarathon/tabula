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
import uk.ac.warwick.tabula.{PermissionDeniedException, RequestInfo, ItemNotFoundException}
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
		with Logging {

	val securityService: SecurityService // abstract dependency

	def requestInfo = RequestInfo.fromThread
	def user = requestInfo.get.user

	// Permissions checking
	if (target.isInstanceOf[PermissionsChecking]) {
		val checkThis = target.asInstanceOf[PermissionsChecking]

		Assert.isTrue(
			!checkThis.permissionsAnyChecks.isEmpty || !checkThis.permissionsAllChecks.isEmpty || target.isInstanceOf[Public],
			"Bind target " + target.getClass + " must specify permissions or extend Public"
		)

		for (check <- checkThis.permissionsAllChecks) check match {
			case (permission: Permission, Some(scope)) => securityService.check(user, permission, scope)
			case (permission: ScopelessPermission, _) => securityService.check(user, permission)
			case _ =>
				// We're trying to do a permissions check against a non-existent scope - 404
				logger.warn("Permissions check throwing item not found - this should be caught in command (" + target + ")")
				throw new ItemNotFoundException()
		}

		if (!checkThis.permissionsAnyChecks.exists ( _ match {
			case (permission: Permission, Some(scope)) => securityService.can(user, permission, scope)
			case (permission: ScopelessPermission, _) => securityService.can(user, permission)
			case _ => {
				// We're trying to do a permissions check against a non-existent scope - 404
				logger.warn("Permissions check throwing item not found - this should be caught in command (" + target + ")")
				false
			}
		})) throw new PermissionDeniedException(user, checkThis.permissionsAnyChecks.head._1, checkThis.permissionsAnyChecks.head._2)
	}
}
