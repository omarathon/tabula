package uk.ac.warwick.courses.web.controllers

import scala.collection.JavaConversions._
import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.Validator
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.WebDataBinder
import uk.ac.warwick.courses.actions.Action
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.services.SecurityService
import uk.ac.warwick.courses.ItemNotFoundException
import uk.ac.warwick.courses.RequestInfo
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.data.model.Feedback
import javax.annotation.Resource
import org.springframework.beans.factory.annotation.Required
import uk.ac.warwick.courses.events.EventHandling
import org.springframework.stereotype.Controller
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.CurrentUser
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.courses.data.model.CanBeDeleted
import uk.ac.warwick.courses.data.Daoisms
import uk.ac.warwick.courses.web.Mav
import uk.ac.warwick.sso.client.tags.SSOLoginLinkGenerator
import uk.ac.warwick.sso.client.SSOConfiguration

abstract trait ControllerMethods extends Logging {
	def mustBeLinked(assignment:Assignment, module:Module) = 
	 if (assignment.module.id != module.id) {
		logger.info("Not displaying assignment as it doesn't belong to specified module")
  		throw new ItemNotFoundException(assignment)
	 }
  
	  def mustBeLinked(feedback:Feedback, assignment:Assignment) = 
		 if (mandatory(feedback).assignment.id != mandatory(assignment).id) {
			logger.info("Not displaying feedback as it doesn't belong to specified assignment")
	  		throw new ItemNotFoundException(feedback)
		 }
	  
	  /**
	   * Returns an object if it is non-null and not None. Otherwise
	   * it throws an ItemNotFoundException, which should get picked
	   * up by an exception handler to display a 404 page.
	   */
	  def mandatory[T](something:T)(implicit m:Manifest[T]):T = something match {
		  case Some(thing:Any) if m.erasure.isInstance(thing) => thing.asInstanceOf[T]
		  case None => throw new ItemNotFoundException()
		  case thing:T if m.erasure.isInstance(thing) => thing.asInstanceOf[T]
		  case _ => throw new ItemNotFoundException()
	  }
	   
	 def notDeleted[T <: CanBeDeleted](entity:T):T = 
		 if (entity.deleted) throw new ItemNotFoundException()
		 else entity
	   
	 def user:CurrentUser
	 var securityService:SecurityService
	 def can(action:Action[_]) = securityService.can(user, action)
	 def mustBeAbleTo(action:Action[_]) = securityService.check(user, action)
}

trait ControllerViews {
	val Mav = uk.ac.warwick.courses.web.Mav
	val Breadcrumbs = uk.ac.warwick.courses.web.Breadcrumbs
	def Redirect(path:String) = Mav("redirect:" + path)
	def RedirectToSignin(target:String=loginUrl):Mav = Redirect(target)
	def Reload() = Redirect(currentPath)
	
	private def currentUri = requestInfo.get.requestedUri
	private def currentPath:String = currentUri.getPath
	private def loginUrl = {
		val generator = new SSOLoginLinkGenerator
		generator.setConfig(SSOConfiguration.getConfig)
		generator.setTarget(currentUri.toString)
		generator.getLoginUrl
	}
	
	def requestInfo:Option[RequestInfo]
}

/**
 * Useful traits for all controllers to have.
 */
@Controller
abstract class BaseController extends ControllerMethods with ControllerViews with ValidatesCommand with Logging with EventHandling with Daoisms {
  // make Mav available to controllers without needing to import
  
  @Required @Resource(name="validator") var globalValidator:Validator =_
  
  @Autowired
  @BeanProperty var securityService:SecurityService =_
  
  var disallowedFields:List[String] = Nil
  
  def requestInfo = RequestInfo.fromThread
  def user = requestInfo.get.user
  def ajax = requestInfo.map{ _.ajax }.getOrElse(false)
  
  /**
   * Enables the Hibernate filter for this session to exclude
   * entities marked as deleted.
   */
  private var _hideDeletedItems = false
  def hideDeletedItems = { _hideDeletedItems = true }
  
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
  @InitBinder final def _binding(binder:WebDataBinder) = {
	  if (validator != null) {
	 	  if (keepOriginalValidator) {
	 	 	  val original = binder.getValidator
	 	 	  binder.setValidator(new CompositeValidator(validator, original))
	 	  } else {
	 		  binder.setValidator(validator)
	 	  }
	  }
	  binder.setDisallowedFields(disallowedFields:_*)
	  binding(binder, binder.getTarget)
  }
  
  /**
   * Do any custom binding init by overriding this method.
   */
  def binding[T](binder:WebDataBinder, target:T) {}
  

}