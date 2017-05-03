package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.commands.cm2.assignments.{AssignmentInfoFilters, ListAssignmentsCommand}
import uk.ac.warwick.tabula.commands.cm2.assignments.ListAssignmentsCommand._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permission
import uk.ac.warwick.tabula.web.Mav

abstract class AbstractDepartmentHomeController
	extends CourseworkController
		with DepartmentScopedController
		with AcademicYearScopedController
		with AutowiringModuleAndDepartmentServiceComponent
		with AutowiringUserSettingsServiceComponent
		with AutowiringMaintenanceModeServiceComponent {

	hideDeletedItems

	override val departmentPermission: Permission = ListAssignmentsCommand.AdminPermission

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] =
		retrieveActiveDepartment(Option(department))

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear], user: CurrentUser): DepartmentCommand = {
		val academicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))

		ListAssignmentsCommand.department(department, academicYear, user)
	}

	@RequestMapping(params=Array("!ajax"), headers=Array("!X-Requested-With"))
	def home(@ModelAttribute("command") command: DepartmentCommand, @PathVariable department: Department): Mav =
		Mav(s"$urlPrefix/admin/home/department",
			"modules" -> command.allModulesWithPermission,
			"allModuleFilters" -> AssignmentInfoFilters.allModuleFilters(command.allModulesWithPermission.sortBy(_.code)),
			"allWorkflowTypeFilters" -> AssignmentInfoFilters.allWorkflowTypeFilters,
			"allStatusFilters" -> AssignmentInfoFilters.Status.all,
			"academicYear" -> command.academicYear
		).secondCrumbs(academicYearBreadcrumbs(command.academicYear)(Routes.admin.department(department, _)): _*)

	@RequestMapping
	def homeAjax(@ModelAttribute("command") command: DepartmentCommand): Mav =
		Mav(s"$urlPrefix/admin/home/moduleList", "modules" -> command.apply()).noLayout()

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}"))
class DepartmentHomeController extends AbstractDepartmentHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] =
		retrieveActiveAcademicYear(None)

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/{academicYear:\\d{4}}"))
class DepartmentHomeForYearController extends AbstractDepartmentHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
		retrieveActiveAcademicYear(Option(academicYear))

}