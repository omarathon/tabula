package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.profiles.commands.TutorTemplateCommand
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController

@Controller
@RequestMapping(value = Array("/department/{department}/tutors/template"))
class TutorTemplateController extends ProfilesController {
	@ModelAttribute def command(@PathVariable("department") department: Department) =
		new TutorTemplateCommand(department)

	@RequestMapping(method = Array(HEAD, GET))
	def generateTemplate(cmd: TutorTemplateCommand) = cmd.apply()
}
