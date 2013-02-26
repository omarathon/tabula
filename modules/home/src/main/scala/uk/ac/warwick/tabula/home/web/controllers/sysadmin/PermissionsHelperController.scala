package uk.ac.warwick.tabula.home.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping

import javax.validation.Valid
import uk.ac.warwick.tabula.helpers.ReflectionHelper
import uk.ac.warwick.tabula.home.commands.sysadmin.PermissionsHelperCommand
import uk.ac.warwick.tabula.permissions._

@Controller
@RequestMapping(Array("/sysadmin/permissions-helper"))
class PermissionsHelperController extends BaseSysadminController {
	
	validatesSelf[PermissionsHelperCommand]
	
	@RequestMapping(method = Array(GET, HEAD))
	def showForm(form: PermissionsHelperCommand, errors: Errors) =
		Mav("sysadmin/permissions-helper/form").noLayoutIf(ajax)

	@RequestMapping(method = Array(POST))
	def submit(@Valid form: PermissionsHelperCommand, errors: Errors) = {	
		if (errors.hasErrors)
			showForm(form, errors)
		else {
			Mav("sysadmin/permissions-helper/results",
				"results" -> form.apply()
			)
		}
	}
	
	@ModelAttribute("allPermissions") def allPermissions = ReflectionHelper.groupedPermissions
	
	@ModelAttribute("allPermissionTargets") def allPermissionTargets = ReflectionHelper.allPermissionTargets

}