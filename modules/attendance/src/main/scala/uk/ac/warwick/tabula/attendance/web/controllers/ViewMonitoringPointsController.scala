package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.web.bind.annotation.{RequestParam, RequestMapping, PathVariable, ModelAttribute}
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.Appliable
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.attendance.commands.{GroupedMonitoringPoint, ViewMonitoringPointsCommand}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint

@Controller
@RequestMapping(value=Array("/{department}"))
class ViewMonitoringPointsController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @RequestParam(value="academicYear", required = false) academicYear: AcademicYear) =
		ViewMonitoringPointsCommand(department, Option(academicYear), user)

	@RequestMapping
	def filter(
		@Valid @ModelAttribute("command") cmd: Appliable[Map[String, Seq[GroupedMonitoringPoint]]],
		errors: Errors,
		@RequestParam(value="updatedMonitoringPoint", required = false) updatedMonitoringPoint: MonitoringPoint
	) = {
		if (errors.hasErrors()) {
			if (ajax)
				Mav("home/view_points_results").noLayout()
			else
				Mav("home/view_points_filter", "updatedMonitoringPoint" -> updatedMonitoringPoint)
		} else {
			val results = cmd.apply()

			if (ajax)
				Mav("home/view_points_results", "pointsMap" -> results).noLayout()
			else
				Mav("home/view_points_filter", "pointsMap" -> results, "updatedMonitoringPoint" -> updatedMonitoringPoint)
		}
	}

}