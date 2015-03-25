package uk.ac.warwick.tabula.exams.web.controllers.admin

import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.web.Routes
import uk.ac.warwick.tabula.exams.web.controllers.ExamsController
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, PermissionDeniedException}

import scala.collection.JavaConverters._

class ExamsAdminDepartmentHomeCommand(val department: Department, val academicYear: AcademicYear, val user: CurrentUser) extends Command[Seq[Module]]
with ReadOnly with Unaudited with AutowiringSecurityServiceComponent with AutowiringModuleAndDepartmentServiceComponent with AutowiringAssessmentServiceComponent {

	val modules: Seq[Module] =
		if (securityService.can(user, Permissions.Module.ManageAssignments, mandatory(department))) {
			// This may seem silly because it's rehashing the above; but it avoids an assertion error where we don't have any explicit permission definitions
			PermissionCheck(Permissions.Module.ManageAssignments, department)

			department.modules.asScala.toSeq
		} else {
			val managedModules = moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.ManageAssignments, department).toList

			// This is implied by the above, but it's nice to check anyway
			PermissionCheckAll(Permissions.Module.ManageAssignments, managedModules)

			if (managedModules.isEmpty)
				throw new PermissionDeniedException(user, Permissions.Module.ManageAssignments, department)

			managedModules
		}

	lazy val modulesAndExams = assessmentService.getExamsByModules(modules, academicYear).withDefaultValue(Seq())

	def applyInternal() = {
		benchmarkTask("Sort modules") { modules.sortBy { module => (modulesAndExams(module).isEmpty, module.code) } }
	}

}

case class ExamsDepartmentHomeInformation(modules: Seq[Module], notices: Map[String, Seq[Exam]])

/**
 * Screens for department and module admins.
 */

@Controller
@RequestMapping(Array("/exams/admin", "/exams/admin/department", "/exams/admin/module"))
class ExamsAdminHomeController extends ExamsController {
	@RequestMapping(method=Array(GET, HEAD))
	def homeScreen(user: CurrentUser) = Redirect(Routes.home)
}

@Controller
@RequestMapping(Array("/exams/admin"))
class ExamsAdminHomeDefaultAcademicYearController extends ExamsController {

	@RequestMapping(value = Array("/department/{department}"), method = Array(GET, HEAD))
	def handleDepartment(@PathVariable department: Department) =
		Redirect(Routes.admin.department(mandatory(department), AcademicYear.guessSITSAcademicYearByDate(DateTime.now)))

	@RequestMapping(value = Array("/module/{module}"), method = Array(GET, HEAD))
	def handleModule(@PathVariable module: Module) =
		Redirect(Routes.admin.module(mandatory(module), AcademicYear.guessSITSAcademicYearByDate(DateTime.now)))
}

@Controller
@RequestMapping(value=Array("/exams/admin/department/{dept}/{academicYear}"))
class ExamsAdminDepartmentHomeController extends ExamsController {

	hideDeletedItems
	
	@ModelAttribute def command(@PathVariable dept: Department, @PathVariable academicYear: AcademicYear, user: CurrentUser) =
		new ExamsAdminDepartmentHomeCommand(dept, academicYear, user)

	@ModelAttribute("academicYears") def academicYearChoices: JList[AcademicYear] =
		AcademicYear.guessSITSAcademicYearByDate(DateTime.now).yearsSurrounding(2, 2).asJava
	
	@RequestMapping
	def adminDepartment(cmd: ExamsAdminDepartmentHomeCommand, @PathVariable dept: Department, @PathVariable academicYear: AcademicYear) = {
		val result = cmd.apply()
		
		Mav("exams/admin/department",
			"department" -> dept,
			"modules" -> result,
			"examMap" -> cmd.modulesAndExams
		).crumbs(
				Breadcrumbs.Department(dept, academicYear)
		)
	}
}

@Controller
@RequestMapping(value=Array("/exams/admin/module/{module}/{academicYear}"))
class ExamsAdminModuleHomeController extends ExamsController {

	hideDeletedItems

	@Autowired var examService: AssessmentService = _
	
	@ModelAttribute("command")
	def command(@PathVariable module: Module, user: CurrentUser) =
		new ViewViewableCommand(Permissions.Module.ManageAssignments, module)

	@ModelAttribute("examMap")
	def examMap(@PathVariable module: Module, @PathVariable academicYear: AcademicYear) =
		examService.getExamsByModules(Seq(module), academicYear).withDefaultValue(Seq())
	
	@RequestMapping
	def adminModule(@ModelAttribute("command") cmd: Appliable[Module], @PathVariable academicYear: AcademicYear) = {
		val module = cmd.apply()
		
		if (ajax) Mav("exams/admin/modules/admin_partial").noLayout()
		else Mav("exams/admin/modules/admin").crumbs(Breadcrumbs.Department(module.adminDepartment, academicYear))
	}
}