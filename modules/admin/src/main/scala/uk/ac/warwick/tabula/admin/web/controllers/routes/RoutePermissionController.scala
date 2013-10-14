package uk.ac.warwick.tabula.admin.web.controllers.routes

import scala.collection.JavaConverters._
import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.permissions.GrantRoleCommand
import uk.ac.warwick.tabula.commands.permissions.RevokeRoleCommand
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.roles.RoleDefinition
import uk.ac.warwick.tabula.admin.web.controllers.AdminController


trait RoutePermissionControllerMethods extends AdminController {

	@ModelAttribute("addCommand") def addCommandModel(@PathVariable("route") route: Route) = new GrantRoleCommand(route)
	@ModelAttribute("removeCommand") def removeCommandModel(@PathVariable("route") route: Route) = new RevokeRoleCommand(route)

	var userLookup = Wire.auto[UserLookupService]

	def form(route: Route): Mav = {
		Mav("admin/routes/permissions", "route" -> route)
			.crumbs(Breadcrumbs.Department(route.department), Breadcrumbs.Route(route))
	}

	def form(route: Route, usercodes: Seq[String], role: Option[RoleDefinition], action: String): Mav = {
		val users = userLookup.getUsersByUserIds(usercodes.asJava)
		Mav("admin/routes/permissions",
				"route" -> route,
				"users" -> users,
				"role" -> role,
				"action" -> action)
			.crumbs(Breadcrumbs.Department(route.department), Breadcrumbs.Route(route))
	}
}

@Controller @RequestMapping(value = Array("/route/{route}/permissions"))
class RoutePermissionController extends AdminController with RoutePermissionControllerMethods {

	@RequestMapping
	def permissionsForm(@PathVariable("route") route: Route, @RequestParam(defaultValue="") usercodes: Array[String],
		@RequestParam(value="role", required=false) role: RoleDefinition, @RequestParam(value="action", required=false) action: String): Mav =
		form(route, usercodes, Some(role), action)
}

@Controller @RequestMapping(value = Array("/route/{route}/permissions"))
class RouteAddPermissionController extends AdminController with RoutePermissionControllerMethods {

	validatesSelf[GrantRoleCommand[_]]

	@RequestMapping(method = Array(POST), params = Array("_command=add"))
	def addPermission(@Valid @ModelAttribute("addCommand") command: GrantRoleCommand[Route], errors: Errors) : Mav = {
		val route = command.scope
		if (errors.hasErrors()) {
			form(route)
		} else {
			val role = Some(command.apply().roleDefinition)
			val userCodes = command.usercodes.asScala
			form(route, userCodes, role, "add")
		}
	}
}

@Controller @RequestMapping(value = Array("/route/{route}/permissions"))
class RouteRemovePermissionController extends AdminController with RoutePermissionControllerMethods {

	validatesSelf[RevokeRoleCommand[_]]

	@RequestMapping(method = Array(POST), params = Array("_command=remove"))
	def removePermission(@Valid @ModelAttribute("removeCommand") command: RevokeRoleCommand[Route],
	                     errors: Errors): Mav = {
		val route = command.scope
		if (errors.hasErrors()) {
			form(route)
		} else {
			val role = Some(command.apply().roleDefinition)
			val userCodes = command.usercodes.asScala
			form(route, userCodes, role, "remove")
		}
	}
}
