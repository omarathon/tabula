package uk.ac.warwick.tabula.admin.web.controllers.department

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.admin.web.controllers.AdminController
import uk.ac.warwick.tabula.data.model.{Module, Route, Department}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.admin.commands.department.AdminDepartmentHomeCommand

@Controller
@RequestMapping(value=Array("/department/{department}"))
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
