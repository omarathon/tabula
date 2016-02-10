package uk.ac.warwick.tabula.web.controllers.attendance.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.web.controllers.attendance.{HasMonthNames, AttendanceController}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.attendance.view.{FilteredStudentsAttendanceResult, FilterStudentsAttendanceCommand}

@Controller
@RequestMapping(Array("/attendance/view/{department}/{academicYear}/students"))
class ViewStudentsAttendanceController extends AttendanceController with HasMonthNames {

	@ModelAttribute("filterCommand")
	def filterCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		FilterStudentsAttendanceCommand(mandatory(department), mandatory(academicYear))

	@RequestMapping
	def home(
		@ModelAttribute("filterCommand") filterCommand: Appliable[FilteredStudentsAttendanceResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear,
		@RequestParam(value="reports", required = false) reports: JInteger,
		@RequestParam(value="monitoringPeriod", required = false) monitoringPeriod: String
	) = {
		val filterResult = filterCommand.apply()
		val modelMap = Map(
			"filterResult" -> filterResult,
			"visiblePeriods" -> filterResult.results.flatMap(_.groupedPointCheckpointPairs.map(_._1)).distinct,
			"reports" -> reports,
			"monitoringPeriod" -> monitoringPeriod
		)
		if (ajax) {
			Mav("attendance/view/_students_results", modelMap).noLayout()
		} else {
			Mav("attendance/view/students", modelMap).crumbs(
				Breadcrumbs.View.Home,
				Breadcrumbs.View.Department(department),
				Breadcrumbs.View.DepartmentForYear(department, academicYear)
			)
		}
	}

}