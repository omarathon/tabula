package uk.ac.warwick.courses.web.controllers
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.ModuleService

@Controller class HomeController {
	@Autowired var moduleService:ModuleService =_
  
	@RequestMapping(Array("/"))	def home = {
	  
	  "home/view"
	}
}