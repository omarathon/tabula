package uk.ac.warwick.tabula.groups.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.{SmallGroupService, ModuleAndDepartmentService}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.groups.SmallGroup

@Controller class HomeController extends GroupsController {
	
	var moduleService = Wire[ModuleAndDepartmentService]
	var smallGroupService = Wire[SmallGroupService]
	
	@RequestMapping(Array("/")) def home(user: CurrentUser) = {
		val ownedDepartments = moduleService.departmentsWithPermission(user, Permissions.Module.ManageSmallGroups)
		val ownedModules = moduleService.modulesWithPermission(user, Permissions.Module.ManageSmallGroups)
		val taughtGroups = smallGroupService.findSmallGroupsByTutor(user.apparentUser)
		
		Mav("home/view",
			"ownedDepartments" -> ownedDepartments,
			"ownedModuleDepartments" -> ownedModules.map { _.department },
			"taughtGroups" -> Nil //taughtGroups
		)
	}
	
}