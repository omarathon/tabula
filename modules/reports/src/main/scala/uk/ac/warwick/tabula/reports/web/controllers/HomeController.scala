package uk.ac.warwick.tabula.reports.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

/**
 * Displays the Reports home screen.
 */
@Controller
@RequestMapping(Array("/"))
class HomeController extends ReportsController {

	@RequestMapping
	def home = {
		Mav("home")
	}

}