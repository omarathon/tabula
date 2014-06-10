package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.manage.{AddTemplatePointsToSchemesCommandState, AddTemplatePointsToSchemesCommand}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.AttendanceMonitoringService
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController


@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/addpoints/template"))
class AddPointsToSchemesFromTemplateController extends AttendanceController  {

	@Autowired var service: AttendanceMonitoringService = _

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) = {
		AddTemplatePointsToSchemesCommand(department, academicYear)
	}

	@RequestMapping(method = Array(POST))
	def post(
		@ModelAttribute("command") cmd: AddTemplatePointsToSchemesCommandState,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		Mav("manage/templates",
			"schemes" -> cmd.schemes,
			"templates" -> cmd.templateSchemeItems,
			"department" -> cmd.schemes.get(0).department,
			"academicYear" -> cmd.academicYear.startYear.toString).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(department),
			Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
			)
	}

	@RequestMapping(method = Array(POST), params = Array("templateScheme"))
	def submit(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringScheme]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		cmd.apply()
		Redirect(Routes.Manage.departmentForYear(department, academicYear))
	}

	@RequestMapping(method = Array(GET))
	def noSchemesSelected(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		Redirect(Routes.Manage.departmentForYear(department, academicYear))

}
