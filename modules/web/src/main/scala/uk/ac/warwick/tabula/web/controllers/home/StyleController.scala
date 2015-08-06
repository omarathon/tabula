package uk.ac.warwick.tabula.web.controllers.home

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.controllers.BaseController

@Controller class StyleController extends BaseController {

	@RequestMapping(Array("/style")) def style() = Mav("style/view")

}