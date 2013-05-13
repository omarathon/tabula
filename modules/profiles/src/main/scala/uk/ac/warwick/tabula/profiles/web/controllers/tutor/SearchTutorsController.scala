package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.validation.Valid
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.profiles.commands.SearchTutorsCommand
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController

@Controller
class SearchTutorsController extends ProfilesController {
	
	@ModelAttribute("searchTutorsCommand") def searchTutorsCommand = new SearchTutorsCommand(user)
		
	@ModelAttribute("student") def student(@RequestParam("student") student: Member) = student
	@ModelAttribute("tutorToDisplay") def tutorToDisplay(@RequestParam(value="currentTutor", required=false) currentTutor: Member) = Option(currentTutor)

	@RequestMapping(value=Array("/tutor/search"), params=Array("!query"))
	def form(@ModelAttribute cmd: SearchTutorsCommand) = Mav("tutor/edit/view", "displayOptionToSave" -> false)

	@RequestMapping(value=Array("/tutor/search"), params=Array("query"))
	def submit(@Valid @ModelAttribute("searchTutorsCommand") cmd: SearchTutorsCommand, errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			Mav("tutor/edit/results",
				"results" -> cmd.apply())
		}
	}

}
