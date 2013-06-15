package uk.ac.warwick.tabula.groups.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.commands.{Appliable, Command, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.groups.web.controllers.GroupsController
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.web.Mav
import scala.collection.JavaConverters._
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet


/**
 * Screens for department and module admins.
 */
@Controller
@RequestMapping(Array("/admin", "/admin/department"))
class AdminHomeController extends GroupsController {
	@RequestMapping(method=Array(GET, HEAD))
	def homeScreen(user: CurrentUser) = Redirect(Routes.home)
}

@Controller
@RequestMapping(value=Array("/admin/department/{department}"))
class AdminDepartmentHomeController extends GroupsController {

	hideDeletedItems
	
	@ModelAttribute("adminDeptCommand") def command(@PathVariable("department") dept: Department, user: CurrentUser) =
		new AdminDepartmentHomeCommand(dept, user)
	
	@ModelAttribute("allocated") def allocatedSet(@RequestParam(value="allocated", required=false) set: SmallGroupSet) = set
	
	@RequestMapping(method=Array(GET, HEAD))
	def adminDepartment(@ModelAttribute("adminDeptCommand") cmd: Appliable[Seq[Module]],
                      @PathVariable("department") department: Department) = {
		val modules = cmd.apply()
		// mapping from module ID to the available group sets.
		val setMapping: Map[String, Seq[SmallGroupSet]] = modules.map {
			module => module.id -> module.groupSets.asScala
		}.toMap

		Mav("admin/department",
			"department" -> department,
			"modules" -> modules,
			"setMapping" -> setMapping,
      "hasUnreleasedGroupsets"->modules.exists(_.hasUnreleasedGroupSets()))
	}
}

class AdminDepartmentHomeCommand(val department: Department, val user: CurrentUser) extends Command[Seq[Module]] with ReadOnly with Appliable[Seq[Module]] with Unaudited {
	
	var securityService = Wire[SecurityService]
	var moduleService = Wire[ModuleAndDepartmentService]
	
	val modules: Seq[Module] = 
		if (securityService.can(user, Permissions.Module.ManageSmallGroups, mandatory(department))) {
			// This may seem silly because it's rehashing the above; but it avoids an assertion error where we don't have any explicit permission definitions
			PermissionCheck(Permissions.Module.ManageSmallGroups, department)
			
			department.modules.asScala
		} else {
			val managedModules = moduleService.modulesWithPermission(user, Permissions.Module.ManageSmallGroups, department).toList
			
			// This is implied by the above, but it's nice to check anyway
			PermissionCheckAll(Permissions.Module.ManageSmallGroups, managedModules)
			
			if (managedModules.isEmpty)
				throw new PermissionDeniedException(user, Permissions.Module.ManageSmallGroups, department)
			
			managedModules
		}
	
	def applyInternal() = modules.sortBy { (module) => (module.groupSets.isEmpty, module.code) }
		
}
