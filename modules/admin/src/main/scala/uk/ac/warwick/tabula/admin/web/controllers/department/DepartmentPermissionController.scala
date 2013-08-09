package uk.ac.warwick.tabula.admin.web.controllers.department

import scala.collection.JavaConverters._
import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.permissions.GrantRoleCommand
import uk.ac.warwick.tabula.commands.permissions.RevokeRoleCommand
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.admin.web.controllers.AdminController

trait DepartmentPermissionControllerMethods extends AdminController {

	@ModelAttribute("addCommand") def addCommandModel(@PathVariable("department") department: Department) = new GrantRoleCommand(department)
	@ModelAttribute("removeCommand") def removeCommandModel(@PathVariable("department") department: Department) = new RevokeRoleCommand(department)

	var userLookup = Wire.auto[UserLookupService]

	def form(department: Department): Mav = {
		Mav("admin/department/permissions",
				"department" -> department)
			.crumbs(Breadcrumbs.Department(department))
	}

	def form(department: Department, usercodes: Seq[String], role: Option[RoleDefinition], action: String): Mav = {
		val users = userLookup.getUsersByUserIds(usercodes.asJava)
		Mav("admin/department/permissions",
				"department" -> department,
				"users" -> users,
				"role" -> role,
				"action" -> action)
			.crumbs(Breadcrumbs.Department(department))
	}
}

@Controller @RequestMapping(value = Array("/department/{department}/permissions"))
class DepartmentPermissionController extends AdminController with DepartmentPermissionControllerMethods {

	@RequestMapping
	def permissionsForm(@PathVariable("department") department: Department, @RequestParam(defaultValue="") usercodes: Array[String],
		@RequestParam(value="role", required=false) role: RoleDefinition, @RequestParam(value="action", required=false) action: String): Mav =
		form(department, usercodes, Some(role), action)


}

@Controller @RequestMapping(value = Array("/department/{department}/permissions"))
class DepartmentAddPermissionController extends AdminController with DepartmentPermissionControllerMethods {

	validatesSelf[GrantRoleCommand[_]]

	@RequestMapping(method = Array(POST), params = Array("_command=add"))
	def addPermission(@Valid @ModelAttribute("addCommand") command: GrantRoleCommand[Department], errors: Errors) : Mav =  {
		val department = command.scope
		if (errors.hasErrors()) {
			form(department)
		} else {
			val role = Some(command.apply().roleDefinition)
			val userCodes = command.usercodes.asScala
			form(department, userCodes, role, "add")
//			val roleName = command.apply().roleDefinition.getName
//			Mav("redirect:" + Routes.department.permissions(department),
//					"role" -> roleName,
//					"usercodes" -> command.usercodes,
//					"action" -> "add"
//			)
		}

	}
}

@Controller @RequestMapping(value = Array("/department/{department}/permissions"))
class DepartmentRemovePermissionController extends AdminController with DepartmentPermissionControllerMethods {

	validatesSelf[RevokeRoleCommand[_]]

	@RequestMapping(method = Array(POST), params = Array("_command=remove"))
	def removePermission(@Valid @ModelAttribute("removeCommand") command: RevokeRoleCommand[Department],
	                     errors: Errors): Mav = {
		val department = command.scope
		if (errors.hasErrors()) {
			form(department)
		} else {
			val role = Some(command.apply().roleDefinition)
			val userCodes = command.usercodes.asScala
			form(department, userCodes, role, "remove")
//			command.apply()
//			val roleName = command.apply().roleDefinition.getName
//			Mav("redirect:" + Routes.department.permissions(department),
//					"role" -> roleName,
//					"usercodes" -> command.usercodes,
//					"action" -> "remove"
//			)
		}

	}
}
