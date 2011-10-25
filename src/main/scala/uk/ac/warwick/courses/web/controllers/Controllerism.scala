package uk.ac.warwick.courses.web.controllers

import uk.ac.warwick.courses._
import uk.ac.warwick.courses.services.SecurityService
import org.springframework.beans.factory.annotation.Autowired
import scala.reflect.BeanProperty

trait Controllerism {
	 
  @Autowired
  @BeanProperty var securityService:SecurityService =_
  
}