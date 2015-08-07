package uk.ac.warwick.tabula.web.controllers.groups.admin

import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

import scala.collection.JavaConverters._
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{Tutor, ViewModules, ViewSet, ViewModule}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.ViewViewableCommand

@Controller
@RequestMapping(value=Array("/groups/admin/module/{module}", "/groups/admin/module/{module}/groups"))
class GroupsAdminModuleHomeController extends GroupsController {

	hideDeletedItems

	@ModelAttribute("adminCommand") def command(@PathVariable("module") module: Module) =
		new ViewViewableCommand(Permissions.Module.ManageSmallGroups, module)

	@RequestMapping
	def adminModule(@ModelAttribute("adminCommand") cmd: Appliable[Module], user: CurrentUser) = {
		val module = cmd.apply()

		// Build the view model
		val moduleItems =
			Seq(
				ViewModule(module,
					module.groupSets.asScala map { set => ViewSet(set, set.groups.asScala.sorted, Tutor) },
					canManageGroups=securityService.can(user, Permissions.Module.ManageSmallGroups, module)
				)
			)

		val data = ViewModules(
			moduleItems,
			canManageDepartment=securityService.can(user, Permissions.Module.ManageSmallGroups, module.adminDepartment)
		)

		if (ajax) Mav("groups/admin/module/sets_partial", "data" -> data ).noLayout()
		else Mav("groups/admin/module/sets", "data" -> data )
	}
}
