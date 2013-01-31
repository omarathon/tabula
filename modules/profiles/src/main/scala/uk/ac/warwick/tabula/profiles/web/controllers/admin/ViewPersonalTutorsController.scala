package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.profiles.commands.ViewPersonalTutorsCommand

@Controller
@RequestMapping(value = Array("/admin/department/{department}/tutors"))
class ViewPersonalTutorsController extends ProfilesController {
	@ModelAttribute def command(@PathVariable("department") department: Department) = new ViewPersonalTutorsCommand(department)

	@RequestMapping(method = Array(HEAD, GET))
	def view(@PathVariable("department") department: Department, @ModelAttribute cmd: ViewPersonalTutorsCommand): Mav = {
		Mav("admin/department/tutors/view", "tutorRelationships" -> cmd.apply)
	}
}
