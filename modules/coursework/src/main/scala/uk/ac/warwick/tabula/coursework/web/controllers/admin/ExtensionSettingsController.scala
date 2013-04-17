package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.Features
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMethod, RequestMapping, PathVariable}
import uk.ac.warwick.tabula.data.model.Department
import scala.Array
import uk.ac.warwick.tabula.coursework.commands.departments.ExtensionSettingsCommand
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes
import org.hibernate.validator.Valid

@Controller
@RequestMapping(Array("/admin/department/{dept}/settings/extensions"))
class ExtensionSettingsController extends CourseworkController {

	var moduleService = Wire[ModuleAndDepartmentService]
	var features = Wire[Features]
	@ModelAttribute def extensionSettingsCommand(@PathVariable("dept") dept:Department) = new ExtensionSettingsCommand(dept, features)
	
	validatesSelf[ExtensionSettingsCommand]

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, dept:Department):Mav = mav.crumbs(Breadcrumbs.Department(dept))

	@RequestMapping(method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def viewSettings(@PathVariable("dept") dept: Department, user: CurrentUser, cmd:ExtensionSettingsCommand, errors:Errors) =
		crumbed(Mav("admin/extension-settings",
			"department" -> dept
		), dept)

	@RequestMapping(method=Array(RequestMethod.POST))
	def saveSettings(@Valid cmd:ExtensionSettingsCommand, errors:Errors) = {
		if (errors.hasErrors){
			viewSettings(cmd.department, user, cmd, errors)
		} else {
			cmd.apply()
			Redirect(Routes.admin.department(cmd.department))
		}
	}
}