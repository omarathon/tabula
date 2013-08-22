package uk.ac.warwick.tabula.attendance.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.stereotype.Controller

@Controller
class HomeController extends BaseController {

	@RequestMapping(Array("/"))
	def home = {
		Mav("home/view")
	}

}