package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.profiles.commands.SearchProfilesCommand
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.stereotype.Controller
import javax.validation.Valid
import uk.ac.warwick.tabula.profiles.helpers.SearchJSONHelpers

@Controller
class SearchController extends ProfilesController with SearchJSONHelpers {

	val formMav = Mav("tutor/edit/view", "displayOptionToSave" -> false)

	@ModelAttribute("searchProfilesCommand") def searchProfilesCommand = new SearchProfilesCommand(currentMember, user)
	
	@RequestMapping(value=Array("/search"), params=Array("!query"))
	def form(@ModelAttribute cmd: SearchProfilesCommand) = formMav
	
	@RequestMapping(value=Array("/search"), params=Array("query"))
	def submitSearch(@Valid @ModelAttribute cmd: SearchProfilesCommand, errors: Errors) = {
		submit(cmd, errors, "profile/search/results")
	}
	
	@RequestMapping(value=Array("/search.json"), params=Array("query"))
	def submitSearchJSON(@Valid @ModelAttribute cmd: SearchProfilesCommand, errors: Errors) = {
		submitJson(cmd, errors)
	}

}