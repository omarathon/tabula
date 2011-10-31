package uk.ac.warwick.courses.web.controllers
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.courses.data.model.Module
import org.springframework.web.bind.annotation.PathVariable

@Controller
class SubmissionController {
  
	@RequestMapping(Array("/module/{module}"))
	def viewModule(@PathVariable module:Module) = Mav("submit/module", "module"->module)
	
}