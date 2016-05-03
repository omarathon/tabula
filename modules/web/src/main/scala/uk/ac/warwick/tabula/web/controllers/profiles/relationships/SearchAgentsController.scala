package uk.ac.warwick.tabula.web.controllers.profiles.relationships

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import javax.validation.Valid
import uk.ac.warwick.tabula.helpers.profiles.SearchJSONHelpers
import uk.ac.warwick.tabula.commands.profiles.SearchAgentsCommand
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController


@Controller
class SearchAgentsController extends ProfilesController with SearchJSONHelpers {

	override val formMav = null

	@ModelAttribute("searchAgentsCommand")
	def searchAgentsCommand = new SearchAgentsCommand(user)

	@RequestMapping(value=Array("/profiles/relationships/agents/search.json"), params=Array("query"))
	def submitAgentSearchJSON(@Valid @ModelAttribute cmd: SearchAgentsCommand, errors: Errors) = {
		submitJson(cmd, errors)
	}

}
