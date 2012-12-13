package uk.ac.warwick.tabula.profiles.web.controllers

import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.profiles.commands.SearchProfilesCommand
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import javax.validation.Valid
import uk.ac.warwick.tabula.profiles.web.ProfileBreadcrumbs

@Controller
@RequestMapping(Array("/search"))
class SearchController extends BaseController with ProfileBreadcrumbs {
	
	hideDeletedItems
	
	@RequestMapping(params=Array("!query"))
	def form(@ModelAttribute cmd: SearchProfilesCommand) = {
		Mav("profile/search/form")
	}
	
	@RequestMapping(params=Array("query"))
	def submit(@Valid @ModelAttribute cmd: SearchProfilesCommand, errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			Mav("profile/search/results",
				"results" -> cmd.apply())
		}
	}

}