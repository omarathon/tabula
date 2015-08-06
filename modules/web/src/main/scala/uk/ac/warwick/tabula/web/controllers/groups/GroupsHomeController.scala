package uk.ac.warwick.tabula.web.controllers.groups

import org.springframework.stereotype.Controller
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.groups.web.views.GroupsDisplayHelper
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.ViewModules
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, SmallGroupService}

@Controller class GroupsHomeController extends GroupsController {
	import GroupsDisplayHelper._

	var moduleService = Wire[ModuleAndDepartmentService]
	var smallGroupService = Wire[SmallGroupService]

	@RequestMapping(Array("/groups")) def home(user: CurrentUser) = {
		if (user.loggedIn) {
			val ownedDepartments = moduleService.departmentsWithPermission(user, Permissions.Module.ManageSmallGroups)
			val ownedModules = moduleService.modulesWithPermission(user, Permissions.Module.ManageSmallGroups)
			val taughtGroups = smallGroupService.findSmallGroupsByTutor(user.apparentUser)
			val memberGroupSets = smallGroupService.findSmallGroupSetsByMember(user.apparentUser)
			val releasedMemberGroupSets = getGroupSetsReleasedToStudents(memberGroupSets)
			val nonEmptyMemberViewModules = getViewModulesForStudent(releasedMemberGroupSets,getGroupsToDisplay(_,user.apparentUser))

			Mav("groups/home/view",
				"ownedDepartments" -> ownedDepartments,
				"ownedModuleDepartments" -> ownedModules.map { _.adminDepartment },
				"taughtGroups" -> taughtGroups,
				"memberGroupsetModules"->ViewModules(nonEmptyMemberViewModules.sortBy(_.module.code), false)
			)
		} else Mav("groups/home/view")
	}

}