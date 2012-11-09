package uk.ac.warwick.courses.web.controllers.admin.modules

import uk.ac.warwick.courses._
import actions.Create
import web.controllers.BaseController
import web.Routes
import commands.modules.AddModuleCommand

import javax.validation.Valid
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller

@Controller
@RequestMapping(value = Array("/admin/module/new"))
class AddModuleController extends BaseController {

	// set up self validation for when @Valid is used
	validatesSelf[AddModuleCommand]

	@RequestMapping(method = Array(HEAD, GET))
	def showForm(cmd: AddModuleCommand, user: CurrentUser) = {
		checkPermissions(user)
		Mav("admin/modules/add/form")
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid cmd: AddModuleCommand, errors: Errors, user: CurrentUser) = {
		checkPermissions(user)
		if (errors.hasErrors) {
			showForm(cmd, user)
		} else {
			val module = cmd.apply()
			Redirect(Routes.admin.module(module))
		}
	}

	def checkPermissions(user: CurrentUser) {
		// sysadmin-only job for now. The intention is to enable this for normaly admins (possibly only on
		// departments where it is enabled) as part of HFC-80
		if (!user.sysadmin) throw new PermissionDeniedException(user, Create())
	}

}
