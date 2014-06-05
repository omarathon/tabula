package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.attendance.commands.manage.AddPointsToSchemesCommand
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/addpoints"))
class AddPointsToSchemesController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		AddPointsToSchemesCommand(department, academicYear)

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[Map[AttendanceMonitoringScheme, Boolean]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam(required = false) points: JInteger
	) = {
		val schemeMap = cmd.apply()
		Mav("manage/addpoints",
			"schemeMap" -> schemeMap,
			"changedSchemes" -> schemeMap.count(_._2),
			"schemesParam" -> schemeMap.filter(_._2).keys.map(s => s"findSchemes=${s.id}").mkString("&"),
			"newPoints" -> Option(points).getOrElse(0)
		).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(department),
			Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
		)
	}

}
