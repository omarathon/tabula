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
import uk.ac.warwick.tabula.coursework.web.Routes

/**
 * Screens for department and module admins.
 */

@Controller
@RequestMapping(Array("/admin", "/admin/department"))
class AdminHomeController extends CourseworkController {
	@RequestMapping(method=Array(GET, HEAD))
	def homeScreen(user: CurrentUser) = Redirect(Routes.home)
}

@Controller
@RequestMapping(value=Array("/admin/department/{dept}"))
class AdminDepartmentHomeController extends CourseworkController {

	hideDeletedItems
	
	@ModelAttribute def command(@PathVariable("dept") dept: Department, user: CurrentUser) =
		new AdminDepartmentHomeCommand(dept, user)
	
	@RequestMapping
	def adminDepartment(cmd: AdminDepartmentHomeCommand) = {
		val info = cmd.apply()
		
		Mav("admin/department",
			"department" -> cmd.department,
			"modules" -> info.modules,
			"notices" -> info.notices)
	}
	
	@RequestMapping(Array("/assignments.xml"))
	def xml(cmd: AdminDepartmentHomeCommand, @PathVariable("dept") dept: Department) = {
		val info = cmd.apply()
		
		new AdminHomeExports.XMLBuilder(dept, info).toXML
	}
}

class AdminDepartmentHomeCommand(val department: Department, val user: CurrentUser) extends Command[DepartmentHomeInformation] with ReadOnly with Unaudited {
	
	var securityService = Wire.auto[SecurityService]
	var moduleService = Wire.auto[ModuleAndDepartmentService]
	
	val modules: JList[Module] = 
		if (securityService.can(user, Permissions.Module.ManageAssignments, mandatory(department))) {
			// This may seem silly because it's rehashing the above; but it avoids an assertion error where we don't have any explicit permission definitions
			PermissionCheck(Permissions.Module.ManageAssignments, department)
			
			department.modules
		} else {
			val managedModules = moduleService.modulesWithPermission(user, Permissions.Module.ManageAssignments, department).toList
			
			// This is implied by the above, but it's nice to check anyway
			PermissionCheckAll(Permissions.Module.ManageAssignments, managedModules)
			
			if (managedModules.isEmpty)
				throw new PermissionDeniedException(user, Permissions.Module.ManageAssignments, department)
			
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
				if assignment.isAlive && assignment.hasUnreleasedFeedback
			) yield assignment
			
		Map(
			"unpublishedAssignments" -> unpublished
		)
	}
	
}

case class DepartmentHomeInformation(modules: Seq[Module], notices: Map[String, Seq[Assignment]])