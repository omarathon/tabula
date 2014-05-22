package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.manage.AddPointsToSchemesCommand
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.JavaImports._

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/new/{scheme}/points"))
class AddPointsToNewSchemeController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		AddPointsToSchemesCommand(department, academicYear)

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[Map[AttendanceMonitoringScheme, Boolean]],
		@PathVariable scheme: AttendanceMonitoringScheme,
		@RequestParam(required = false) points: JInteger
	) = {
		Mav("manage/addpointsoncreate",
			"scheme" -> scheme,
			"newPoints" -> Option(points).getOrElse(0)
		).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(scheme.department),
			Breadcrumbs.Manage.DepartmentForYear(scheme.department, scheme.academicYear)
		)
	}

}
