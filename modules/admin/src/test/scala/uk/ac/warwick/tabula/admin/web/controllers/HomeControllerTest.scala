package uk.ac.warwick.tabula.admin.web.controllers

import uk.ac.warwick.tabula.{Fixtures, TestBase, Mockito}
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.permissions.Permissions

class HomeControllerTest extends TestBase with Mockito {

	trait Fixture {
		val dept1 = Fixtures.department("in")
		val dept2 = Fixtures.department("cs")

		val mod1 = Fixtures.module("in101")
		mod1.department = dept1

		val mod2 = Fixtures.module("in102")
		mod2.department = dept1

		val mod3 = Fixtures.module("cs118")
		mod3.department = dept2

		val route1 = Fixtures.route("i100")
		route1.department = dept1

		val route2 = Fixtures.route("g500")
		route2.department = dept2

		val route3 = Fixtures.route("g503")
		route3.department = dept2

		val controller = new HomeController
		controller.moduleService = mock[ModuleAndDepartmentService]
	}

	@Test def itWorks = withUser("cuscav") { new Fixture {
		controller.moduleService.departmentsWithPermission(currentUser, Permissions.Module.Administer) returns (Set(dept1))
		controller.moduleService.departmentsWithPermission(currentUser, Permissions.Route.Administer) returns (Set(dept1))
		controller.moduleService.modulesWithPermission(currentUser, Permissions.Module.Administer) returns (Set(mod1, mod2, mod3))
		controller.moduleService.routesWithPermission(currentUser, Permissions.Route.Administer) returns (Set(route1, route2, route3))

		val mav = controller.home(currentUser)
		mav.viewName should be ("home/view")
		mav.toModel should be (Map(
			"ownedDepartments" -> Set(dept1),
			"ownedModuleDepartments" -> Set(dept1, dept2),
			"ownedRouteDepartments" -> Set(dept1, dept2)
		))
	}}

}
