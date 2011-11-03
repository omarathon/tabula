package uk.ac.warwick.courses.web.controllers

import scala.reflect.BeanProperty

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.courses.services.SecurityService
import uk.ac.warwick.courses.ItemNotFoundException

/**
 * Useful traits for all controllers to have.
 */
trait Controllerism extends Logging {
	 
  @Autowired
  @BeanProperty var securityService:SecurityService =_
  
  /**
   * Returns an object if it is non-null and not None. Otherwise
   * it throws an ItemNotFoundException, which should get picked
   * up by an exception handler to display a 404 page.
   */
  def definitely[T](something:Any):T = something match {
	  case Some(anything:T) => anything
	  case None => throw new ItemNotFoundException()
	  case anything:T => anything
	  case _ => throw new ItemNotFoundException()
  }
  
}