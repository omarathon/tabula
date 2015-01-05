package uk.ac.warwick.tabula.home.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.services.{CourseAndRouteService, ModuleAndDepartmentService}
import uk.ac.warwick.tabula.web.controllers._

@Controller class HomeController extends BaseController {
	
	var moduleService = Wire[ModuleAndDepartmentService]
	var routeService = Wire[CourseAndRouteService]
	var permissionsService = Wire[PermissionsService]

	hideDeletedItems

	@RequestMapping(Array("/")) def home(user: CurrentUser) = {
		val canDeptAdmin = user.loggedIn && moduleService.departmentsWithPermission(user, Permissions.Department.Reports).nonEmpty
		val canAdmin = canDeptAdmin ||
			// Avoid doing too much work by just returning the first one of these that's true
			user.loggedIn && (
				moduleService.departmentsWithPermission(user, Permissions.Module.Administer).nonEmpty ||
				moduleService.departmentsWithPermission(user, Permissions.Route.Administer).nonEmpty ||
				moduleService.modulesWithPermission(user, Permissions.Module.Administer).nonEmpty ||
				routeService.routesWithPermission(user, Permissions.Route.Administer).nonEmpty
			)

		val canViewProfiles =
			user.isStaff ||
			user.isStudent ||
			permissionsService.getAllPermissionDefinitionsFor(user, Permissions.Profiles.ViewSearchResults).nonEmpty
		
	  Mav("home/view",
	  	"ajax" -> ajax,
			"canAdmin" -> canAdmin,
			"canDeptAdmin" -> canDeptAdmin,
			"canViewProfiles" -> canViewProfiles,
			"jumbotron" -> true
		).noLayoutIf(ajax) // All hail our new Jumbotron overlords
	}
}