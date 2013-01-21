package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

import uk.ac.warwick.tabula.actions.Manage
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.profiles.commands.UploadPersonalTutorsCommand
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(value = Array("/admin/department/{department}/tutors"))
class UploadPersonalTutorsController extends ProfilesController {

	@ModelAttribute def command = new UploadPersonalTutorsCommand

	@RequestMapping(method = Array(HEAD, GET))
	def uploadForm(@PathVariable department: Department, @ModelAttribute cmd: UploadPersonalTutorsCommand): Mav = {
		mustBeAbleTo(Manage(department))
		Mav("admin/department/tutors/uploadform")
	}

	@RequestMapping(method = Array(POST), params = Array("!confirm"))
	def confirmBatchUpload(@PathVariable department: Department, @ModelAttribute cmd: UploadPersonalTutorsCommand, errors: Errors): Mav = {
		bindAndValidate(department, cmd, errors)
		Mav("admin/department/tutors/uploadpreview")
	}

	@RequestMapping(method = Array(POST), params = Array("confirm=true"))
	def doUpload(@PathVariable department: Department, @ModelAttribute cmd: UploadPersonalTutorsCommand, errors: Errors): Mav = {
		bindAndValidate(department, cmd, errors)
		cmd.apply()
		Mav("admin/department/tutors/uploadform")
	}

	private def bindAndValidate(department: Department, cmd: UploadPersonalTutorsCommand, errors: Errors) {
		mustBeAbleTo(Manage(department))
		cmd.onBind
		cmd.postExtractValidation(errors, department)
	}
}