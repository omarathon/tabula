package uk.ac.warwick.tabula.web.controllers.admin

import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{CourseAndRouteService, ModuleAndDepartmentService}
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class HomeControllerTest extends TestBase with Mockito {

	trait Fixture {
		val dept1 = Fixtures.department("in")
		val dept2 = Fixtures.department("cs")

		val mod1 = Fixtures.module("in101")
		mod1.adminDepartment = dept1

		val mod2 = Fixtures.module("in102")
		mod2.adminDepartment = dept1

		val mod3 = Fixtures.module("cs118")
		mod3.adminDepartment = dept2

		val route1 = Fixtures.route("i100")
		route1.adminDepartment = dept1

		val route2 = Fixtures.route("g500")
		route2.adminDepartment = dept2

		val route3 = Fixtures.route("g503")
		route3.adminDepartment = dept2

		val controller = new AdminHomeController
		controller.moduleAndDepartmentService = smartMock[ModuleAndDepartmentService]
		controller.courseAndRouteService = smartMock[CourseAndRouteService]
	}

	@Test def itWorks() = withUser("cuscav") { new Fixture {
		controller.moduleAndDepartmentService.departmentsWithPermission(currentUser, Permissions.Module.Administer) returns Set(dept1)
		controller.moduleAndDepartmentService.departmentsWithPermission(currentUser, Permissions.Route.Administer) returns Set(dept1)
		controller.moduleAndDepartmentService.modulesWithPermission(currentUser, Permissions.Module.Administer) returns Set(mod1, mod2, mod3)
		controller.courseAndRouteService.routesWithPermission(currentUser, Permissions.Route.Administer) returns Set(route1, route2, route3)

		val mav = controller.home(None)
		mav.viewName should be ("admin/home/view")
		mav.toModel should be (Map(
			"ownedDepartments" -> Set(dept1),
			"ownedModuleDepartments" -> Set(dept1, dept2),
			"ownedRouteDepartments" -> Set(dept1, dept2)
		))
	}}

}
