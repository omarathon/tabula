package uk.ac.warwick.tabula.groups.web.controllers.admin

import scala.collection.JavaConverters._
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.data.model.{Module, Department}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.commands.admin.AdminDepartmentHomeCommand
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{Tutor, ViewModules, ViewSet, ViewModule}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.groups.commands.admin.ViewDepartmentAttendanceCommand

@Controller
@RequestMapping(value=Array("/admin/department/{department}/attendance"))
class ViewDepartmentAttendanceController extends GroupsController {

	hideDeletedItems
	
	@ModelAttribute("adminCommand") def command(@PathVariable("department") dept: Department, user: CurrentUser) =
		ViewDepartmentAttendanceCommand(dept, user)

	@RequestMapping(method=Array(GET, HEAD))
	def adminDepartment(@ModelAttribute("adminCommand") cmd: Appliable[Seq[Module]], @PathVariable("department") dept: Department) = {
		val modules = cmd.apply()

		Mav("groups/attendance/view_department",
			"modules" -> modules).crumbs(Breadcrumbs.Department(dept))
	}
}
