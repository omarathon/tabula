package uk.ac.warwick.tabula.coursework.web.controllers.admin.modules
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.RequestMapping

import javax.validation.Valid

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.coursework.commands.modules.AddModuleCommand
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController

@Controller
@RequestMapping(value = Array("/admin/module/new"))
class AddModuleController extends CourseworkController {

	// set up self validation for when @Valid is used
	validatesSelf[AddModuleCommand]

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(cmd: AddModuleCommand, user: CurrentUser) = {
		Mav("admin/modules/add/form")
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid cmd: AddModuleCommand, errors: Errors, user: CurrentUser) = {
		if (errors.hasErrors) {
			showForm(cmd, user)
		} else {
			val module = cmd.apply()
			Redirect(Routes.admin.module(module))
		}
	}

}
