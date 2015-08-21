package uk.ac.warwick.tabula.web.controllers.admin

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{Department, Module, Route}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._

trait AdminDepartmentsModulesAndRoutes {

	self: ModuleAndDepartmentServiceComponent with CourseAndRouteServiceComponent =>

	case class Result(departments: Set[Department], modules: Set[Module], routes: Set[Route])

	def ownedDepartmentsModulesAndRoutes(user: CurrentUser): Result = {
		val ownedDepartments =
			moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Module.Administer) ++
				moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Route.Administer)
		val ownedModules = moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.Administer)
		val ownedRoutes = courseAndRouteService.routesWithPermission(user, Permissions.Route.Administer)
		Result(ownedDepartments, ownedModules, ownedRoutes)
	}
}

@Controller
class AdminHomeController extends AdminController with AdminDepartmentsModulesAndRoutes
	with AutowiringModuleAndDepartmentServiceComponent with AutowiringCourseAndRouteServiceComponent {
	
	@RequestMapping(Array("/admin")) def home(user: CurrentUser) = {
		val result = ownedDepartmentsModulesAndRoutes(user)
		
		Mav("admin/home/view",
			"ownedDepartments" -> result.departments,
			"ownedModuleDepartments" -> result.modules.map { _.adminDepartment },
			"ownedRouteDepartments" -> result.routes.map { _.adminDepartment })
	}

}