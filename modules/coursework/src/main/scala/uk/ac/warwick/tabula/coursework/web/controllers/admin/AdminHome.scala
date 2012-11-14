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
import uk.ac.warwick.tabula.coursework.actions.Manage
import uk.ac.warwick.tabula.coursework.actions.Participate
import uk.ac.warwick.tabula.coursework.commands.assignments._
import uk.ac.warwick.tabula.coursework.commands.feedback._
import uk.ac.warwick.tabula.coursework.data.model._
import uk.ac.warwick.tabula.coursework.data.FeedbackDao
import uk.ac.warwick.tabula.coursework.services.fileserver.FileServer
import uk.ac.warwick.tabula.coursework.services._
import uk.ac.warwick.tabula.coursework.web.controllers.BaseController
import uk.ac.warwick.tabula.coursework.web.Routes
import uk.ac.warwick.tabula.coursework.AcademicYear
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.coursework.ItemNotFoundException
import uk.ac.warwick.tabula.coursework.services.AuditEventIndexService

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
		val modules: JList[Module] = if (isDeptManager) {
			dept.modules
		} else {
			moduleService.modulesManagedBy(user.idForPermissions, dept).toList
		}
		if (modules.isEmpty()) {
			mustBeAbleTo(Manage(dept))
		}
		Mav("admin/department",
			"department" -> dept,
			"modules" -> modules.sortBy { (module) => (module.assignments.isEmpty, module.code) })

	}

}
