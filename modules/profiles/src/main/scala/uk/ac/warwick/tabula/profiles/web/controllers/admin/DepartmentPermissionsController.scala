package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.commands.permissions.RevokeRoleCommand
import uk.ac.warwick.tabula.commands.permissions.GrantRoleCommand
import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.data.model.Department
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import javax.validation.Valid
import uk.ac.warwick.tabula.roles.ModuleManagerRoleDefinition
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.Breadcrumbs


trait DepartmentPermissionsControllerMethods extends BaseController {

	@ModelAttribute("addCommand") def addCommandModel(@PathVariable("department") department: Department) = new GrantRoleCommand(department)
	@ModelAttribute("removeCommand") def removeCommandModel(@PathVariable("department") department: Department) = new RevokeRoleCommand(department)
	
	def form(department: Department): Mav = {
		Mav("permissions/department/form", "department" -> department)
	}
}

@Controller @RequestMapping(value = Array("/admin/department/{department}/permissions"))
class DepartmentPermissionsController extends BaseController with DepartmentPermissionsControllerMethods {
	@RequestMapping
	def permissionsForm(@PathVariable("department") department: Department): Mav =
		form(department)
}

@Controller @RequestMapping(value = Array("/admin/department/{department}/permissions"))
class ModuleAddPermissionController extends BaseController with DepartmentPermissionsControllerMethods {

	validatesSelf[GrantRoleCommand[_]]
	
	@RequestMapping(method = Array(POST), params = Array("_command=add"))
	def addPermission(@Valid @ModelAttribute("addCommand") command: GrantRoleCommand[Department], errors: Errors): Mav = {
		val department = command.scope
		if (errors.hasErrors()) {
			println(errors)
			form(department)
		} else {
			command.apply()
			Redirect(Routes.admin.departmentPermissions(department))
		}

	}
}

@Controller @RequestMapping(value = Array("/admin/department/{department}/permissions"))
class ModuleRemovePermissionController extends BaseController with DepartmentPermissionsControllerMethods {
	
	validatesSelf[RevokeRoleCommand[_]]
	
	@RequestMapping(method = Array(POST), params = Array("_command=remove"))
	def addPermission(@Valid @ModelAttribute("removeCommand") command: RevokeRoleCommand[Department], errors: Errors): Mav = {
		val department = command.scope
		if (errors.hasErrors()) {
			form(department)
		} else {
			command.apply()
			Redirect(Routes.admin.departmentPermissions(department))
		}

	}
}
