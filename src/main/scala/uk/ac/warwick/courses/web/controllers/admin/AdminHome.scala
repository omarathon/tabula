package uk.ac.warwick.courses.web.controllers.admin

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
import uk.ac.warwick.courses.JavaImports._
import uk.ac.warwick.courses.actions.Manage
import uk.ac.warwick.courses.actions.Participate
import uk.ac.warwick.courses.commands.assignments._
import uk.ac.warwick.courses.commands.feedback._
import uk.ac.warwick.courses.data.model._
import uk.ac.warwick.courses.data.FeedbackDao
import uk.ac.warwick.courses.services.fileserver.FileServer
import uk.ac.warwick.courses.services._
import uk.ac.warwick.courses.web.controllers.BaseController
import uk.ac.warwick.courses.web.Routes
import uk.ac.warwick.courses.AcademicYear
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.ItemNotFoundException
import uk.ac.warwick.courses.services.AuditEventIndexService

/**
 * Screens for department and module admins.
 */

@Controller
class AdminHome extends BaseController {

	@Autowired var moduleService: ModuleAndDepartmentService = _
	
	hideDeletedItems

	@RequestMapping(Array("/admin/"))
	def homeScreen(user: CurrentUser) = {
		Mav("admin/home",
			"ownedDepartments" -> moduleService.departmentsOwnedBy(user.idForPermissions))
	}

	@RequestMapping(Array("/admin/department/{dept}/"))
	def adminDepartment(@PathVariable dept: Department, user: CurrentUser) = {
		val isDeptManager = can(Manage(mandatory(dept)))
		val modules:JList[Module] = if (isDeptManager) {
			dept.modules
		} else {
			moduleService.modulesManagedBy(user.idForPermissions, dept).toList
		}
		if (modules.isEmpty()) {
			mustBeAbleTo(Manage(dept))
		}
		Mav("admin/department",
			"department" -> dept,
			"modules" -> modules.sortBy{ (module) => (module.assignments.isEmpty, module.code) })
			
	}
	
}
