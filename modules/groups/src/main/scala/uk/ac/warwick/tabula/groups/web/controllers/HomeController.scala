package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.{SmallGroupService, ModuleAndDepartmentService}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.groups.web.views.GroupsDisplayHelper
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.ViewModules

@Controller class HomeController extends GroupsController {
	import GroupsDisplayHelper._

	var moduleService = Wire[ModuleAndDepartmentService]
	var smallGroupService = Wire[SmallGroupService]

	@RequestMapping(Array("/")) def home(user: CurrentUser) = {
		if (user.loggedIn) {
			val ownedDepartments = moduleService.departmentsWithPermission(user, Permissions.Module.ManageSmallGroups)
			val ownedModules = moduleService.modulesWithPermission(user, Permissions.Module.ManageSmallGroups)
			val taughtGroups = smallGroupService.findSmallGroupsByTutor(user.apparentUser)
			val memberGroupSets = smallGroupService.findSmallGroupSetsByMember(user.apparentUser)		
			val releasedMemberGroupSets = getGroupSetsReleasedToStudents(memberGroupSets)
			val nonEmptyMemberViewModules = getViewModulesForStudent(releasedMemberGroupSets,getGroupsToDisplay(_,user.apparentUser))
	
			Mav("home/view",
				"ownedDepartments" -> ownedDepartments,
				"ownedModuleDepartments" -> ownedModules.map { _.department },
				"taughtGroups" -> taughtGroups,
				"memberGroupsetModules"->ViewModules(nonEmptyMemberViewModules.sortBy(_.module.code), false)
			)
		} else Mav("home/view")
	}

}