package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{CurrentUser, PermissionDeniedException}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._

import scala.collection.JavaConversions._

/**
 * Screens for department and module admins.
 */

@Controller
@RequestMapping(Array("/admin", "/admin/department", "/admin/module"))
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
			"modules" -> info
		)
	}
	
	@RequestMapping(Array("/assignments.xml"))
	def xml(cmd: AdminDepartmentHomeCommand, @PathVariable("dept") dept: Department) = {
		val info = cmd.apply()
		
		new AdminHomeExports.XMLBuilder(dept, DepartmentHomeInformation(info, cmd.gatherNotices(info))).toXML
	}
}

@Controller
@RequestMapping(value=Array("/admin/module/{module}"))
class AdminModuleHomeController extends CourseworkController {

	hideDeletedItems
	
	@ModelAttribute("command") def command(@PathVariable("module") module: Module, user: CurrentUser) =
		new ViewViewableCommand(Permissions.Module.ManageAssignments, module)
	
	@RequestMapping
	def adminModule(@ModelAttribute("command") cmd: Appliable[Module]) = {
		val module = cmd.apply()
		
		if (ajax) Mav("admin/modules/admin_partial").noLayout()
		else Mav("admin/modules/admin").crumbs(Breadcrumbs.Department(module.adminDepartment))
	}
}

class AdminDepartmentHomeCommand(val department: Department, val user: CurrentUser) extends Command[Seq[Module]]
		with ReadOnly with Unaudited {
	
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
		modules.sortBy { (module) => (module.assignments.isEmpty, module.code) }
	}
	
	def gatherNotices(modules: Seq[Module]) = benchmarkTask("Gather notices") {
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