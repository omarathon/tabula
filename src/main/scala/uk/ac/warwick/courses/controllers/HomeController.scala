package uk.ac.warwick.courses.controllers
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class HomeController {
	@RequestMapping(Array("/"))
	def home = "home/view"
}