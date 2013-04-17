package uk.ac.warwick.tabula.web.controllers

import uk.ac.warwick.spring.Wire
import org.springframework.context.MessageSource
import org.springframework.stereotype.Controller
import org.springframework.validation.Validator
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.events.EventHandling
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.validators.CompositeValidator
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.sso.client.SSOConfiguration
import uk.ac.warwick.sso.client.tags.SSOLoginLinkGenerator
import org.springframework.web.servlet.view.RedirectView
import uk.ac.warwick.tabula.system.permissions.PermissionsCheckingMethods
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking

abstract trait ControllerMethods extends PermissionsCheckingMethods with Logging {
	def user: CurrentUser
	var securityService: SecurityService
	
	def restricted[A <: PermissionsChecking](something: => A): Option[A] = 
		if (something.permissionChecks forall(_ match {
			case (permission: Permission, Some(scope)) => securityService.can(user, permission, scope)
			case (permission: ScopelessPermission, _) => securityService.can(user, permission)
			case _ =>
				// We're trying to do a permissions check against a non-existent scope - 404
				logger.warn("Permissions check throwing item not found - this should be caught in check (restricted " + something + ")")
				throw new ItemNotFoundException()
		})) Some(something)
		else None
		
	def restrictedBy[A <: PermissionsChecking](fn: => Boolean)(something: => A): Option[A] =
		if (fn) restricted(something)
		else Some(something)
}

trait ControllerViews {
	val Mav = uk.ac.warwick.tabula.web.Mav
	
	def Redirect(path: String) = requestInfo.flatMap { _.requestParameters.get("returnTo") } match {
		case Some(returnTo :: tail) => Mav("redirect:" + returnTo)
		case _ => Mav("redirect:" + path) 
	}

	def RedirectToSignin(target: String = loginUrl): Mav = Redirect(target)

	private def currentUri = requestInfo.get.requestedUri
	private def currentPath: String = currentUri.getPath
	def loginUrl = {
		val generator = new SSOLoginLinkGenerator
		generator.setConfig(SSOConfiguration.getConfig)
		generator.setTarget(currentUri.toString)
		generator.getLoginUrl
	}

	def requestInfo: Option[RequestInfo]
}

trait ControllerImports {
	import org.springframework.web.bind.annotation.RequestMethod
	final val GET = RequestMethod.GET
	final val PUT = RequestMethod.PUT
	final val HEAD = RequestMethod.HEAD
	final val POST = RequestMethod.POST

	type RequestMapping = org.springframework.web.bind.annotation.RequestMapping
}

trait PreRequestHandler {
	def preRequest
}

/**
 * Useful traits for all controllers to have.
 */
@Controller
abstract class BaseController extends ControllerMethods
	with ControllerViews
	with ValidatesCommand
	with Logging
	with EventHandling
	with Daoisms
	with StringUtils
	with ControllerImports
	with PreRequestHandler {

	var globalValidator = Wire[Validator]("validator")

	var securityService = Wire[SecurityService]

	private var messageSource = Wire[MessageSource]

	/**
	 * Resolve a message from messages.properties. This is the same way that
	 * validation error codes are resolved.
	 */
	def getMessage(key: String, args: Object*) = messageSource.getMessage(key, args.toArray, null)

	var disallowedFields: List[String] = Nil

	def requestInfo = RequestInfo.fromThread
	def user = requestInfo.get.user
	def ajax = requestInfo.map { _.ajax }.getOrElse(false)

	/**
	 * Enables the Hibernate filter for this session to exclude
	 * entities marked as deleted.
	 */
	private var _hideDeletedItems = false
	def hideDeletedItems = { _hideDeletedItems = true }
	def showDeletedItems = { _hideDeletedItems = false }

	final def preRequest {
		// if hideDeletedItems has been called, exclude all "deleted=1" items from Hib queries.
		if (_hideDeletedItems) {
			session.enableFilter("notDeleted")
		}
		
		onPreRequest
	}
	
	// Stub implementation that can be overridden for logic that goes before a request
	def onPreRequest {}

	/**
	 * Sets up @Valid validation.
	 * If "validator" has been set, it will be used. If "keepOriginalValidator" is true,
	 * it will be joined up with the default global validator (the one that does annotation based
	 * validation like @NotEmpty). Otherwise it's replaced.
	 *
	 * Sets up disallowedFields.
	 */
	@InitBinder final def _binding(binder: WebDataBinder) = {
		if (validator != null) {
			if (keepOriginalValidator) {
				val original = binder.getValidator
				binder.setValidator(new CompositeValidator(validator, original))
			} else {
				binder.setValidator(validator)
			}
		}
		binder.setDisallowedFields(disallowedFields: _*)
		binding(binder, binder.getTarget)
	}

	/**
	 * Do any custom binding init by overriding this method.
	 */
	def binding[A](binder: WebDataBinder, target: A) {}

}