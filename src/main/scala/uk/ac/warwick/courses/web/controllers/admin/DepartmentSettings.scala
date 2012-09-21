package uk.ac.warwick.courses.web.controllers.admin

import org.springframework.stereotype.Controller
import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.ModuleAndDepartmentService
import uk.ac.warwick.courses.CurrentUser
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMethod, RequestMapping, PathVariable}
import uk.ac.warwick.courses.data.model.Department
import uk.ac.warwick.courses.actions.Manage
import scala.Array
import uk.ac.warwick.courses.commands.departments.DepartmentSettingsCommand
import org.springframework.validation.Errors
import uk.ac.warwick.courses.web.{Mav, Routes}

@Controller
@RequestMapping(Array("/admin/department/{dept}/settings"))
class DepartmentSettings extends BaseController {

	@Autowired var moduleService: ModuleAndDepartmentService = _
	@ModelAttribute def departmentSettingsCommand(@PathVariable dept:Department) = new DepartmentSettingsCommand(dept)

	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, dept:Department):Mav = mav.crumbs(Breadcrumbs.Department(dept))

	@RequestMapping(method=Array(RequestMethod.GET, RequestMethod.HEAD))
	def viewSettings(@PathVariable dept: Department, user: CurrentUser, cmd:DepartmentSettingsCommand, errors:Errors) = {
		cmd.copySettings()
		mustBeAbleTo(Manage(dept))
		val model = Mav("admin/department-settings",
			"department" -> dept
		)
		crumbed(model, dept)
	}

	@RequestMapping(method=Array(RequestMethod.POST))
	def saveSettings(@PathVariable dept:Department, user:CurrentUser, cmd:DepartmentSettingsCommand, errors:Errors) = {
		mustBeAbleTo(Manage(dept))
		cmd.validate(errors)
		if (errors.hasErrors){
			viewSettings(dept,user,cmd,errors)
		}
		else{
			cmd.apply()
			Redirect(Routes.admin.department(dept))
		}
	}
}