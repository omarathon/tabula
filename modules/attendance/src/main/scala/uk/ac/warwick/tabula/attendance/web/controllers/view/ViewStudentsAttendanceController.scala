package uk.ac.warwick.tabula.attendance.web.controllers.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.attendance.web.controllers.{HasMonthNames, AttendanceController}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.attendance.commands.view.{FilterStudentsAttendanceCommandResult, FilterStudentsAttendanceCommand}

@Controller
@RequestMapping(Array("/view/{department}/{academicYear}/students"))
class ViewStudentsAttendanceController extends AttendanceController with HasMonthNames {

	@ModelAttribute("filterCommand")
	def filterCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		FilterStudentsAttendanceCommand(department, academicYear, user)

	@RequestMapping
	def home(
		@ModelAttribute("filterCommand") filterCommand: Appliable[FilterStudentsAttendanceCommandResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val filterResult = filterCommand.apply()
		if (ajax) {
			Mav("view/_students_results",
				"filterResult" -> filterResult,
				"visiblePeriods" -> filterResult.results.flatMap(_.groupedPointCheckpointPairs.map(_._1)).distinct
			).noLayout()
		} else {
			Mav("view/students",
				"filterResult" -> filterResult,
				"visiblePeriods" -> filterResult.results.flatMap(_.groupedPointCheckpointPairs.map(_._1)).distinct
			).crumbs(
				Breadcrumbs.View.Home,
				Breadcrumbs.View.Department(department),
				Breadcrumbs.View.DepartmentForYear(department, academicYear)
			)
		}

	}

}