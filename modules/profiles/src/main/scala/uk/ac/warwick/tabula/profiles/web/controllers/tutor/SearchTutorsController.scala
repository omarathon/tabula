package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import javax.validation.Valid
import uk.ac.warwick.tabula.profiles.commands.SearchTutorsCommand
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.profiles.helpers.SearchJSONHelpers


@Controller
class SearchTutorsController extends ProfilesController with SearchJSONHelpers {

	val formMav = Mav("tutor/edit/view", "displayOptionToSave" -> false)

	@ModelAttribute("searchTutorsCommand") def searchTutorsCommand = new SearchTutorsCommand(user)

	@RequestMapping(value=Array("/tutor/search"), params=Array("!query"))
	def form(@ModelAttribute cmd: SearchTutorsCommand) = formMav

	@RequestMapping(value=Array("/tutor/search"), params=Array("query"))
	def submitTutorSearch(@Valid @ModelAttribute("searchTutorsCommand") cmd: SearchTutorsCommand, errors: Errors) = {
	 	submit(cmd, errors, "tutor/edit/results")
	}

	@RequestMapping(value=Array("/tutor/search.json"), params=Array("query"))
	def submitTutorSearchJSON(@Valid @ModelAttribute cmd: SearchTutorsCommand, errors: Errors) = {
		submitJson(cmd, errors)
	}

}
