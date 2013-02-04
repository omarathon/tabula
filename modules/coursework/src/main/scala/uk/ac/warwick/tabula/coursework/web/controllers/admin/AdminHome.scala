package uk.ac.warwick.tabula.coursework.web.controllers.admin

import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.coursework.commands.assignments._
import uk.ac.warwick.tabula.coursework.commands.feedback._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.PermissionDeniedException

/**
 * Screens for department and module admins.
 */

@Controller
class AdminHome extends CourseworkController {

	@Autowired var moduleService: ModuleAndDepartmentService = _

	hideDeletedItems
	
	@ModelAttribute def command(@PathVariable("dept") dept: Department, user: CurrentUser) =
		new AdminDepartmentHomeCommand(dept, user)

	@RequestMapping(Array("/admin/"))
	def homeScreen(user: CurrentUser) = {
		Mav("admin/home",
			"ownedDepartments" -> moduleService.departmentsOwnedBy(user.idForPermissions))
	}

	@RequestMapping(Array("/admin/department/{dept}/"))
	def adminDepartment(cmd: AdminDepartmentHomeCommand) = {
		val info = cmd.apply()
		
		Mav("admin/department",
			"department" -> cmd.department,
			"modules" -> info.modules,
			"notices" -> info.notices)

	}

}

/**
 * This command has the Public trait, which is semantically wrong - but it does its permissions checking in-line to handle module managers.
 * 
 * If we didn't have the Public trait, we'd throw an assertion error for module managers.
 */
class AdminDepartmentHomeCommand(val department: Department, val user: CurrentUser) extends Command[DepartmentHomeInformation] with ReadOnly with Unaudited with Public {
	
	var securityService = Wire.auto[SecurityService]
	var moduleService = Wire.auto[ModuleAndDepartmentService]
	
	val modules: JList[Module] = 
		if (securityService.can(user, Permissions.Module.Read, mandatory(department))) department.modules
		else {
			val managedModules = moduleService.modulesManagedBy(user.idForPermissions, department).toList
			
			// This is implied by the above, but it's nice to check anyway
			PermissionCheckAll(Permissions.Module.Read, managedModules)
			
			if (managedModules.isEmpty)
				throw new PermissionDeniedException(user, Permissions.Module.Read, department)
			
			managedModules
		}
	
	def applyInternal() = {
		val sortedModules = modules.sortBy { (module) => (module.assignments.isEmpty, module.code) }
		val notices = gatherNotices(modules)
		
		new DepartmentHomeInformation(sortedModules, notices)
	}
	
	def gatherNotices(modules: Seq[Module]) = {
		val unpublished = for ( 
				module <- modules;
				assignment <- module.assignments
				if assignment.isAlive && assignment.anyUnreleasedFeedback
			) yield assignment
			
		Map(
			"unpublishedAssignments" -> unpublished
		)
	}
	
}

case class DepartmentHomeInformation(modules: Seq[Module], notices: Map[String, Seq[Assignment]])