package uk.ac.warwick.courses.web.controllers

import scala.reflect.BeanProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.services.SecurityService
import uk.ac.warwick.courses.ItemNotFoundException
import uk.ac.warwick.courses.RequestInfo
import uk.ac.warwick.courses.actions.Action

/**
 * Useful traits for all controllers to have.
 */
trait Controllerism extends Logging {
	 
  @Autowired
  @BeanProperty var securityService:SecurityService =_
  
  def requestInfo = RequestInfo.fromThread
  def user = requestInfo.get.user
  def mustBeAbleTo(action:Action) = securityService.check(user, action)
  
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
  
}