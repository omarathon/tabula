package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.profiles.commands.ViewPersonalTutorsCommand
import uk.ac.warwick.tabula.profiles.commands.ViewPersonalTuteesCommand
import uk.ac.warwick.tabula.profiles.commands.UploadPersonalTutorsCommand

@Controller
@RequestMapping(value = Array("/department/{department}/tutors"))
class ViewPersonalTutorsController extends ProfilesController {
	@ModelAttribute("viewPersonalTutorsCommand") def viewPersonalTutorsCommand(@PathVariable("department") department: Department) =
		new ViewPersonalTutorsCommand(department)

	@RequestMapping(method = Array(HEAD, GET))
	def view(@PathVariable("department") department: Department, @ModelAttribute("viewPersonalTutorsCommand") cmd: ViewPersonalTutorsCommand): Mav = {
		Mav("tutors/tutor_view",
			"tutorRelationships" -> cmd.apply,
			"department" -> department
		)
	}
}

@Controller
@RequestMapping(value = Array("/tutees"))
class ViewPersonalTuteesController extends ProfilesController {
	@ModelAttribute("cmd") def command = new ViewPersonalTuteesCommand(currentMember)

	@RequestMapping(method = Array(HEAD, GET))
	def view(@ModelAttribute("cmd") cmd: ViewPersonalTuteesCommand): Mav = {
		Mav("tutors/tutee_view", "tutees" -> cmd.apply)
	}
}
