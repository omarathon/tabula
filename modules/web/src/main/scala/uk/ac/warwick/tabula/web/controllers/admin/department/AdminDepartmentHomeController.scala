package uk.ac.warwick.tabula.web.controllers.admin.department

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.web.controllers.admin.AdminController
import uk.ac.warwick.tabula.data.model.{Module, Route, Department}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.admin.department.AdminDepartmentHomeCommand

@Controller
@RequestMapping(value=Array("/admin/department/{department}"))
class AdminDepartmentHomeController extends AdminController {

	hideDeletedItems

	type AdminDepartmentHomeCommand = Appliable[(Seq[Module], Seq[Route])]

	@ModelAttribute def command(@PathVariable("department") dept: Department, user: CurrentUser): AdminDepartmentHomeCommand =
		AdminDepartmentHomeCommand(dept, user)

	@RequestMapping
	def adminDepartment(cmd: AdminDepartmentHomeCommand, @PathVariable("department") dept: Department) = {
		val (modules, routes) = cmd.apply()

		Mav("admin/department",
			"department" -> dept,
			"modules" -> modules,
			"departmentRoutes" -> routes) // Stupid workaround for "routes" being used already in Freemarker
	}
}

@Controller
@RequestMapping(Array("/admin/department"))
class AdminDepartmentHomeRedirectController extends AdminController {
	@RequestMapping
	def homeScreen(user: CurrentUser) = Redirect(Routes.admin.home)
}