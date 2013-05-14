package uk.ac.warwick.tabula.coursework.web.controllers.admin

import scala.collection.JavaConversions._
import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.commands.permissions.GrantRoleCommand
import uk.ac.warwick.tabula.commands.permissions.RevokeRoleCommand
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.roles.RoleDefinition


trait ModulePermissionControllerMethods extends CourseworkController {

	@ModelAttribute("addCommand") def addCommandModel(@PathVariable("module") module: Module) = new GrantRoleCommand(module)
	@ModelAttribute("removeCommand") def removeCommandModel(@PathVariable("module") module: Module) = new RevokeRoleCommand(module)

	var userLookup = Wire.auto[UserLookupService]

	def form(module: Module): Mav = {
		Mav("admin/modules/permissions/form", "module" -> module)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

	def form(module: Module, usercodes: Array[String], role: Option[RoleDefinition], action: String): Mav = {
		val users = userLookup.getUsersByUserIds(usercodes.toList)
		Mav("admin/modules/permissions/form",
				"module" -> module,
				"users" -> users,
				"role" -> role,
				"action" -> action)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}
}

@Controller @RequestMapping(value = Array("/admin/module/{module}/permissions"))
class ModulePermissionController extends CourseworkController with ModulePermissionControllerMethods {

	@RequestMapping
	def permissionsForm(@PathVariable("module") module: Module, @RequestParam(defaultValue="") usercodes: Array[String],
		@RequestParam(value="role", required=false) role: RoleDefinition, @RequestParam(value="action", required=false) action: String): Mav =
		form(module, usercodes, Some(role), action)


}

@Controller @RequestMapping(value = Array("/admin/module/{module}/permissions"))
class ModuleAddPermissionController extends CourseworkController with ModulePermissionControllerMethods {

	validatesSelf[GrantRoleCommand[_]]
	
	@RequestMapping(method = Array(POST), params = Array("_command=add"))
	def addPermission(@Valid @ModelAttribute("addCommand") command: GrantRoleCommand[Module], errors: Errors) : Mav =  {
		val module = command.scope
		if (errors.hasErrors()) {
			form(module)
		} else {
			val roleName = command.apply().roleDefinition.getName
			Mav("redirect:" + Routes.admin.modulePermissions(module),
					"role" -> roleName,
					"usercodes" -> command.usercodes,
					"action" -> "add"
			)
		}

	}
}

@Controller @RequestMapping(value = Array("/admin/module/{module}/permissions"))
class ModuleRemovePermissionController extends CourseworkController with ModulePermissionControllerMethods {

	validatesSelf[RevokeRoleCommand[_]]
	
	@RequestMapping(method = Array(POST), params = Array("_command=remove"))
	def removePermission(@Valid @ModelAttribute("removeCommand") command: RevokeRoleCommand[Module],
	                     errors: Errors): Mav = {
		val module = command.scope
		if (errors.hasErrors()) {
			form(module)
		} else {
			command.apply()
			val roleName = command.apply().roleDefinition.getName
			Mav("redirect:" + Routes.admin.modulePermissions(module),
					"role" -> roleName,
					"usercodes" -> command.usercodes,
					"action" -> "remove"
			)
		}

	}
}
