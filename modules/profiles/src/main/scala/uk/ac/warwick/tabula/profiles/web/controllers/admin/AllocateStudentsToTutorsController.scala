package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.data.model.Department
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import javax.validation.Valid
import uk.ac.warwick.tabula.profiles.commands.tutor.AllocateStudentsToTutorsCommand


/**
 * Allocates students to tutors in a department.
 */
@Controller
@RequestMapping(value=Array("/department/{department}/tutors/allocate"))
class AllocateStudentsToTutorsController extends ProfilesController {
	
	validatesSelf[AllocateStudentsToTutorsCommand]
	
	@ModelAttribute
	def command(@PathVariable department: Department) = new AllocateStudentsToTutorsCommand(department, user)

	@RequestMapping
	def showForm(cmd: AllocateStudentsToTutorsCommand) = {
		cmd.populate()
		cmd.sort()
		form(cmd)
	}
	
	@RequestMapping(method=Array(POST), params=Array("action=refresh"))
	def form(cmd: AllocateStudentsToTutorsCommand) = Mav("tutors/allocate")

	@RequestMapping(method=Array(POST), params=Array("action!=refresh"))
	def submit(@Valid cmd:AllocateStudentsToTutorsCommand, errors: Errors): Mav = {
		cmd.sort()
		if (errors.hasErrors()) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect("/") // TODO
		}
	}

}