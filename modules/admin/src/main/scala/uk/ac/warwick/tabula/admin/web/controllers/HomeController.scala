package uk.ac.warwick.tabula.admin.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller

@Controller class HomeController extends BaseController {
	
	@RequestMapping(Array("/")) def home() = Mav("home/view")
	
}