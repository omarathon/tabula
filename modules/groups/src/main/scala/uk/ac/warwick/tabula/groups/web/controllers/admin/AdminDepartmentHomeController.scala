package uk.ac.warwick.tabula.groups.web.controllers.admin

import scala.collection.JavaConverters._

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.groups.commands.admin.AdminDepartmentHomeCommand
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{ViewModules, ViewSet, ViewModule}
import uk.ac.warwick.tabula.permissions.Permissions


@Controller
@RequestMapping(value=Array("/admin/department/{department}"))
class AdminDepartmentHomeController extends GroupsController {

	hideDeletedItems

	@ModelAttribute def command(@PathVariable("department") dept: Department, user: CurrentUser) =
		new AdminDepartmentHomeCommand(dept, user)

	@ModelAttribute("allocated") def allocatedSet(@RequestParam(value="allocated", required=false) set: SmallGroupSet) = set

	@RequestMapping(method=Array(GET, HEAD))
	def adminDepartment(cmd: AdminDepartmentHomeCommand, user: CurrentUser) = {
		val modules = cmd.apply()

		// Build the view model
		val moduleItems =
			for (module <- modules) yield {
				ViewModule(module,
					module.groupSets.asScala map { ViewSet(_) },
					canManageGroups=securityService.can(user, Permissions.Module.ManageSmallGroups, module)
				)
			}

		val data = ViewModules(
			moduleItems.toSeq,
			canManageDepartment=securityService.can(user, Permissions.Module.ManageSmallGroups, cmd.department)
		)



		Mav("admin/department",
			"department" -> cmd.department,
			"data" -> data )
	}
}
