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

@Controller
@RequestMapping(value=Array("/admin/department/{department}"))
class AdminDepartmentHomeController extends GroupsController {

	hideDeletedItems

	@ModelAttribute("adminCommand") def command(@PathVariable("department") dept: Department, user: CurrentUser) =
		AdminDepartmentHomeCommand(dept, user)

	@ModelAttribute("allocated") def allocatedSet(@RequestParam(value="allocated", required=false) set: SmallGroupSet) = set

	@RequestMapping
	def adminDepartment(@ModelAttribute("adminCommand") cmd: Appliable[Seq[Module]], @PathVariable("department") department:Department, user: CurrentUser) = {
		val modules = cmd.apply()

		// Build the view model
		val moduleItems =
			for (module <- modules) yield {
				ViewModule(module,
					module.groupSets.asScala map { set => ViewSet(set, set.groups.asScala, Tutor) },
					canManageGroups=securityService.can(user, Permissions.Module.ManageSmallGroups, module)
				)
			}

		val data = ViewModules(
			moduleItems.toSeq,
			canManageDepartment=securityService.can(user, Permissions.Module.ManageSmallGroups, department)
		)

		val hasModules = !moduleItems.isEmpty
		val hasGroups = moduleItems.exists { _.module.groupSets.asScala.exists { g => !g.deleted && !g.archived } }
		val hasGroupAttendance = moduleItems.exists { _.module.groupSets.asScala.exists { g => g.showAttendanceReports } }

		Mav("admin/department",
			"department" -> department,
			"data" -> data,
			"hasModules" -> hasModules,
			"hasGroups" -> hasGroups,
			"hasGroupAttendance" -> hasGroupAttendance)
	}
}
