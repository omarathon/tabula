package uk.ac.warwick.tabula.home.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller

@Controller class StyleController extends BaseController {
	
	@RequestMapping(Array("/style")) def style() = Mav("style/view") 

}