package uk.ac.warwick.courses.web.controllers.admin

import org.springframework.stereotype.Controller
import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import uk.ac.warwick.courses.{Features, CurrentUser}
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMethod, RequestMapping, PathVariable}
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.actions.Manage
import scala.Array
import uk.ac.warwick.courses.commands.departments.ExtensionSettingsCommand
import org.springframework.validation.Errors
import uk.ac.warwick.courses.web.{Mav, Routes}

@Controller
@RequestMapping(Array("/admin/department/{dept}/settings/extensions"))
class ExtensionSettingsController extends BaseController {

	@Autowired var moduleService: ModuleAndDepartmentService = _
	@Autowired var features: Features = _
	@ModelAttribute def extensionSettingsCommand(@PathVariable dept:Department) = new ExtensionSettingsCommand(dept, features)

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, dept:Department):Mav = mav.crumbs(Breadcrumbs.Department(dept))

	@RequestMapping(method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def viewSettings(@PathVariable dept: Department, user: CurrentUser, cmd:ExtensionSettingsCommand, errors:Errors) = {
		cmd.copySettings()
		mustBeAbleTo(Manage(dept))
		val model = Mav("admin/extension-settings",
			"department" -> dept
		)
		crumbed(model, dept)
	}

	@RequestMapping(method=Array(RequestMethod.POST))
	def saveSettings(cmd:ExtensionSettingsCommand, errors:Errors) = {
		mustBeAbleTo(Manage(cmd.department))
		cmd.validate(errors)
		if (errors.hasErrors){
			viewSettings(cmd.department, user, cmd, errors)
		}
		else{
			cmd.apply()
			Redirect(Routes.admin.department(cmd.department))
		}
	}
}