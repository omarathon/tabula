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

/**
 * Useful traits for all controllers to have.
 */
trait Controllerism extends ValidatesCommand with Logging {
	
  // make Mav available to controllers without needing to import
  val Mav = uk.ac.warwick.courses.web.Mav
	
  @Autowired
  @BeanProperty var securityService:SecurityService =_
  
  var disallowedFields:List[String] = Nil
  
  def requestInfo = RequestInfo.fromThread
  def user = requestInfo.get.user
  def mustBeAbleTo(action:Action[_]) = securityService.check(user, action)
  
  def mustBeLinked(assignment:Assignment, module:Module) = 
	 if (assignment.module.id != module.id) {
		logger.info("Not displaying assignment as it doesn't belong to specified module")
  		throw new ItemNotFoundException(assignment)
	 }
  
  def mustBeLinked(feedback:Feedback, assignment:Assignment) = 
	 if (feedback.assignment.id != assignment.id) {
		logger.info("Not displaying feedback as it doesn't belong to specified assignment")
  		throw new ItemNotFoundException(feedback)
	 }
  
  /**
   * Returns an object if it is non-null and not None. Otherwise
   * it throws an ItemNotFoundException, which should get picked
   * up by an exception handler to display a 404 page.
   */
  def definitely[T](something:Any)(implicit m:Manifest[T]):T = something match {
	  case Some(thing:Any) if m.erasure.isInstance(thing) => thing.asInstanceOf[T]
	  case None => throw new ItemNotFoundException()
	  case thing:Any if m.erasure.isInstance(thing) => thing.asInstanceOf[T]
	  case _ => throw new ItemNotFoundException()
  }
  
  @InitBinder def _binding(binder:WebDataBinder) = {
	  if (validator != null) {
	 	  if (keepOriginalValidator) {
	 	 	  val original = binder.getValidator
	 	 	  binder.setValidator(new CompositeValidator(validator, original))
	 	  } else {
	 		  binder.setValidator(validator)
	 	  }
	  }
	  binder.setDisallowedFields(disallowedFields:_*)
	  binding(binder)
  }
  def binding(binder:WebDataBinder) {}

}