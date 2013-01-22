package uk.ac.warwick.tabula.coursework.web.controllers.admin

import javax.persistence.Entity
import javax.persistence.NamedQueries
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Configurable
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation._
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.seqAsJavaList
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.actions.Manage
import uk.ac.warwick.tabula.actions.Participate
import uk.ac.warwick.tabula.coursework.commands.assignments._
import uk.ac.warwick.tabula.coursework.commands.feedback._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.FeedbackDao
import uk.ac.warwick.tabula.services.fileserver.FileServer
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions.Public

/**
 * Screens for department and module admins.
 */

@Controller
class AdminHome extends CourseworkController {

	@Autowired var moduleService: ModuleAndDepartmentService = _

	hideDeletedItems
	
	@ModelAttribute def command(@PathVariable dept: Department, user: CurrentUser) =
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
		if (securityService.can(user, Manage(mandatory(department)))) department.modules
		else moduleService.modulesManagedBy(user.idForPermissions, department).toList
		
	if (modules.isEmpty) {
		PermissionsCheck(Manage(department))
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