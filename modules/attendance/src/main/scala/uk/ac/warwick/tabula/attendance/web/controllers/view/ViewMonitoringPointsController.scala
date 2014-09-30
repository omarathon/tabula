package uk.ac.warwick.tabula.attendance.web.controllers.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.view.{FilterMonitoringPointsCommand, FilterMonitoringPointsCommandResult}
import uk.ac.warwick.tabula.attendance.web.controllers.{AttendanceController, HasMonthNames}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department

@Controller
@RequestMapping(value=Array("/view/{department}/{academicYear}/points"))
class ViewMonitoringPointsController extends AttendanceController with HasMonthNames {

	@ModelAttribute("filterCommand")
	def filterCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		FilterMonitoringPointsCommand(mandatory(department), mandatory(academicYear), user)

	@RequestMapping
	def home(
		@ModelAttribute("filterCommand") filterCommand: Appliable[FilterMonitoringPointsCommandResult],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		val filterResult = filterCommand.apply().pointMap
		if (ajax) {
			Mav("view/_points_results",
				"filterResult" -> filterResult
			).noLayout()
		} else {
			Mav("view/points",
				"filterResult" -> filterResult
			).crumbs(
				Breadcrumbs.View.Home,
				Breadcrumbs.View.Department(department),
				Breadcrumbs.View.DepartmentForYear(department, academicYear)
			)
		}
	}

}