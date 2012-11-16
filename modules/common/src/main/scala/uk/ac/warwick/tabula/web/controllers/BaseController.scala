package uk.ac.warwick.tabula.web.controllers

import scala.reflect.BeanProperty

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Required
import org.springframework.context.MessageSource
import org.springframework.stereotype.Controller
import org.springframework.validation.Validator
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.RequestMethod

import javax.annotation.Resource
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.actions.Action
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

abstract trait ControllerMethods extends Logging {
	def mustBeLinked(assignment: Assignment, module: Module) =
		if (mandatory(assignment).module.id != mandatory(module).id) {
			logger.info("Not displaying assignment as it doesn't belong to specified module")
			throw new ItemNotFoundException(assignment)
		}

	def mustBeLinked(feedback: Feedback, assignment: Assignment) =
		if (mandatory(feedback).assignment.id != mandatory(assignment).id) {
			logger.info("Not displaying feedback as it doesn't belong to specified assignment")
			throw new ItemNotFoundException(feedback)
		}
	
	def mustBeLinked(markScheme: MarkScheme, department: Department) =
		if (mandatory(markScheme).department.id != mandatory(department.id)) {
			logger.info("Not displaying mark scheme as it doesn't belong to specified department")
			throw new ItemNotFoundException(markScheme)
		}

	/**
	 * Returns an object if it is non-null and not None. Otherwise
	 * it throws an ItemNotFoundException, which should get picked
	 * up by an exception handler to display a 404 page.
	 */
	def mandatory[T](something: T)(implicit m: Manifest[T]): T = something match {
		case thing: Any if m.erasure.isInstance(thing) => thing.asInstanceOf[T]
		case _ => throw new ItemNotFoundException()
	}
	/**
	 * Pass in an Option and receive either the actual value, or
	 * an ItemNotFoundException is thrown.
	 */
	def mandatory[T](option: Option[T])(implicit m: Manifest[T]): T = option match {
		case Some(thing: Any) if m.erasure.isInstance(thing) => thing.asInstanceOf[T]
		case _ => throw new ItemNotFoundException()
	}

	def notDeleted[T <: CanBeDeleted](entity: T): T =
		if (entity.deleted) throw new ItemNotFoundException()
		else entity

	def user: CurrentUser
	var securityService: SecurityService
	def can(action: Action[_]) = securityService.can(user, action)
	def mustBeAbleTo(action: Action[_]) = securityService.check(user, action)
}

trait ControllerViews {
	val Mav = uk.ac.warwick.tabula.web.Mav
	
	def Redirect(path: String) = Mav("redirect:" + path)
	def RedirectToSignin(target: String = loginUrl): Mav = Redirect(target)
	def Reload() = Redirect(currentPath)

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

	@Required @Resource(name = "validator") var globalValidator: Validator = _

	@Autowired
	@BeanProperty var securityService: SecurityService = _

	@Autowired private var messageSource: MessageSource = _

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
	}

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
	def binding[T](binder: WebDataBinder, target: T) {}

}