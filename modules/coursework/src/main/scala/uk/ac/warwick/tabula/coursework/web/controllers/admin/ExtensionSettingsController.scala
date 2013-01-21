package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.Features
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMethod, RequestMapping, PathVariable}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.actions.Manage
import scala.Array
import uk.ac.warwick.tabula.coursework.commands.departments.ExtensionSettingsCommand
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes

@Controller
@RequestMapping(Array("/admin/department/{dept}/settings/extensions"))
class ExtensionSettingsController extends CourseworkController {

	@Autowired var moduleService: ModuleAndDepartmentService = _
	@Autowired var features: Features = _
	@ModelAttribute def extensionSettingsCommand(@PathVariable dept:Department) = new ExtensionSettingsCommand(dept, features)

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, dept:Department):Mav = mav.crumbs(Breadcrumbs.Department(dept))

	@RequestMapping(method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def viewSettings(@PathVariable dept: Department, user: CurrentUser, cmd:ExtensionSettingsCommand, errors:Errors) = {
		if(!errors.hasErrors){
			cmd.copySettings()
		}
		val model = Mav("admin/extension-settings",
			"department" -> dept
		)
		crumbed(model, dept)
	}

	@RequestMapping(method=Array(RequestMethod.POST))
	def saveSettings(cmd:ExtensionSettingsCommand, errors:Errors) = {
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