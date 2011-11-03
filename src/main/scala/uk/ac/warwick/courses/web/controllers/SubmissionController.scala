package uk.ac.warwick.courses.web.controllers
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

import javax.persistence.Entity
import javax.persistence.NamedQueries
import uk.ac.warwick.courses.data.model.Module

@Controller
class SubmissionController extends Controllerism {
  
	@RequestMapping(Array("/module/{module}"))
	def viewModule(@PathVariable module:Module) = Mav("submit/module", 
			"module"-> definitely(module))
	
}