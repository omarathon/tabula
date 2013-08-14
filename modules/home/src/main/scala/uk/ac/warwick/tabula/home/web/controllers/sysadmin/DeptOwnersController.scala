package uk.ac.warwick.tabula.home.web.controllers.sysadmin

import scala.collection.JavaConverters._
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import javax.validation.Valid
import uk.ac.warwick.tabula.commands.permissions.GrantRoleCommand
import uk.ac.warwick.tabula.commands.permissions.RevokeRoleCommand
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.roles.{RoleDefinition, DepartmentalAdministratorRoleDefinition}
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/sysadmin/departments/"))
class DeptDetailsController extends BaseSysadminController {

	@RequestMapping
	def departments = Mav("sysadmin/departments/list",
		"departments" -> moduleService.allDepartments.sortBy{ _.name })
		.crumbs(Breadcrumbs.Current("Sysadmin department list"))

	@RequestMapping(Array("/{dept}/"))
	def department(@PathVariable("dept") dept: Department) = {
		Mav("sysadmin/departments/single",
			"department" -> dept)
			.crumbs(Breadcrumbs.Current(s"Sysadmin ${dept.name} overview"))
	}

}

trait DepartmentPermissionControllerMethods extends BaseSysadminController {

	@ModelAttribute("addCommand") def addCommandModel(@PathVariable("department") department: Department) =
		new GrantRoleCommand(department, DepartmentalAdministratorRoleDefinition)

	@ModelAttribute("removeCommand") def removeCommandModel(@PathVariable("department") department: Department) =
		new RevokeRoleCommand(department, DepartmentalAdministratorRoleDefinition)

	def form(@PathVariable("department") department: Department): Mav = {
		Mav("sysadmin/departments/permissions", "department" -> department)
			.crumbs(Breadcrumbs.Current(s"Sysadmin ${department.name} permissions"))
	}

	def form(department: Department, usercodes: Seq[String], role: Option[RoleDefinition], action: String): Mav = {
		val users = userLookup.getUsersByUserIds(usercodes.asJava)
		Mav("sysadmin/departments/permissions",
			"department" -> department,
			"users" -> users,
			"role" -> role,
			"action" -> action)
			.crumbs(Breadcrumbs.Current(s"Sysadmin ${department.name} permissions"))
	}

	def redirectToDeptPermissions(deptcode: String) = Mav("redirect:/sysadmin/departments/" + deptcode + "/permissions")
}

@Controller @RequestMapping(Array("/sysadmin/departments/{department}/permissions"))
class DepartmentPermissionController extends BaseSysadminController with DepartmentPermissionControllerMethods {
	@RequestMapping
	def permissionsForm(@PathVariable("department") department: Department): Mav =
		form(department)
}

@Controller @RequestMapping(Array("/sysadmin/departments/{department}/permissions"))
class DepartmentAddPermissionController extends BaseSysadminController with DepartmentPermissionControllerMethods {

	validatesSelf[GrantRoleCommand[_]]

	@RequestMapping(method = Array(POST), params = Array("_command=add"))
	def addPermission(@Valid @ModelAttribute("addCommand") command: GrantRoleCommand[Department], errors: Errors): Mav = {
		val department = command.scope
		if (errors.hasErrors()) {
			form(department)
		} else {
			command.apply()
			redirectToDeptPermissions(department.code)
		}

	}
}

@Controller @RequestMapping(Array("/sysadmin/departments/{department}/permissions"))
class DepartmentRemovePermissionController extends BaseSysadminController with DepartmentPermissionControllerMethods {

	validatesSelf[RevokeRoleCommand[_]]

	@RequestMapping(method = Array(POST), params = Array("_command=remove"))
	def addPermission(@Valid @ModelAttribute("removeCommand") command: RevokeRoleCommand[Department], errors: Errors): Mav = {
		val department = command.scope
		if (errors.hasErrors()) {
			form(department)
		} else {
			command.apply()
			redirectToDeptPermissions(department.code)
		}

	}
}