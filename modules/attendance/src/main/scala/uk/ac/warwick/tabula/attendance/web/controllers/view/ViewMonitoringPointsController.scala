package uk.ac.warwick.tabula.attendance.web.controllers.view

import org.springframework.web.bind.annotation.{RequestMapping, PathVariable, ModelAttribute}
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.controllers.{HasMonthNames, AttendanceController}
import uk.ac.warwick.tabula.attendance.commands.view.FilterMonitoringPointsCommand
import uk.ac.warwick.tabula.attendance.commands.GroupedPoint

@Controller
@RequestMapping(value=Array("/view/{department}/{academicYear}/points"))
class ViewMonitoringPointsController extends AttendanceController with HasMonthNames {

	@ModelAttribute("filterCommand")
	def filterCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		FilterMonitoringPointsCommand(department, academicYear, user)

	@RequestMapping
	def home(
		@ModelAttribute("filterCommand") filterCommand: Appliable[Map[String, Seq[GroupedPoint]]],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear) = {
			val filterResult = filterCommand.apply()
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