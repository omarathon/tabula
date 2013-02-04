package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.profiles.commands.UploadPersonalTutorsCommand
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.system.BindListener
import org.hibernate.validator.Valid

@Controller
@RequestMapping(value = Array("/admin/department/{department}/tutors"))
class UploadPersonalTutorsController extends ProfilesController {
	// tell @Valid annotation how to validate
	validatesSelf[UploadPersonalTutorsCommand]
	
	@ModelAttribute def command(@PathVariable("department") department: Department) = new UploadPersonalTutorsCommand(department)

	@RequestMapping(method = Array(HEAD, GET))
	def uploadForm(@PathVariable("department") department: Department, @ModelAttribute cmd: UploadPersonalTutorsCommand): Mav = {
		Mav("admin/department/tutors/uploadform")
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmBatchUpload(@PathVariable("department") department: Department, @Valid @ModelAttribute cmd: UploadPersonalTutorsCommand, errors: Errors): Mav = {
		//validate(department, cmd, errors)
		Mav("admin/department/tutors/uploadpreview")
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(@PathVariable("department") department: Department, @Valid @ModelAttribute cmd: UploadPersonalTutorsCommand, errors: Errors): Mav = {
		//validate(department, cmd, errors)
		val tutorCount = cmd.apply().size
		Mav("admin/department/tutors/uploadform", "tutorCount" -> tutorCount)
	}
}
