package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceMonitoringPointType}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.attendance.commands.manage.{FindPointsResult, FindPointsCommand}
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.AttendanceMonitoringService
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.attendance.commands.manage.FindPointsResult

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/editpoints"))
class SelectAttendancePointsToEditController extends AttendanceController {

	@Autowired var attendanceMonitoringService: AttendanceMonitoringService = _

	@ModelAttribute("findCommand")
	def findCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		FindPointsCommand(department, academicYear, None)

	@ModelAttribute("allSchemes")
	def allSchemes(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		attendanceMonitoringService.listSchemes(department, academicYear)

	@RequestMapping
	def home(
		@ModelAttribute("findCommand") findCommand: Appliable[FindPointsResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam(required = false) points: JInteger
	) = {
		val findCommandResult = findCommand.apply()
		Mav("manage/editpoints",
			"findResult" -> findCommandResult,
			"allTypes" -> AttendanceMonitoringPointType.values,
			"allStyles" -> AttendanceMonitoringPointStyle.values,
			"newPoints" -> Option(points).getOrElse(0)
		).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(department),
			Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
		)

	}

}
