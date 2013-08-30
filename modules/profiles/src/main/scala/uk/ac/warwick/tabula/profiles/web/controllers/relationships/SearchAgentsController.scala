package uk.ac.warwick.tabula.profiles.web.controllers.relationships

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import javax.validation.Valid
import uk.ac.warwick.tabula.profiles.commands.SearchAgentsCommand
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.profiles.helpers.SearchJSONHelpers


@Controller
class SearchAgentsController extends ProfilesController with SearchJSONHelpers {

	val formMav = Mav("relationships/edit/view", "displayOptionToSave" -> false)

	@ModelAttribute("searchAgentsCommand") def searchAgentsCommand = new SearchAgentsCommand(user)

	@RequestMapping(value=Array("/relationships/agents/search"), params=Array("!query"))
	def form(@ModelAttribute cmd: SearchAgentsCommand) = formMav

	@RequestMapping(value=Array("/relationships/agents/search"), params=Array("query"))
	def submitAgentSearch(@Valid @ModelAttribute("searchAgentsCommand") cmd: SearchAgentsCommand, errors: Errors) = {
	 	submit(cmd, errors, "relationships/edit/results")
	}

	@RequestMapping(value=Array("/relationships/agents/search.json"), params=Array("query"))
	def submitAgentSearchJSON(@Valid @ModelAttribute cmd: SearchAgentsCommand, errors: Errors) = {
		submitJson(cmd, errors)
	}

}
