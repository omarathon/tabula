package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{Route, Department}
import uk.ac.warwick.tabula.attendance.commands.{ViewMonitoringPointSetsCommand, ManageMonitoringPointSetCommand}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}

/**
 * Displays the screen for viewing monitoring points for a department
 */
@Controller
@RequestMapping(Array("/{dept}"))
class ViewMonitoringPointsForDeptController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(
		@PathVariable dept: Department,
		@RequestParam(value="academicYear", required = false) academicYear: AcademicYear,
		@RequestParam(value="route", required = false) route: Route,
		@RequestParam(value="set", required = false) set: MonitoringPointSet
	) =
			ViewMonitoringPointSetsCommand(dept, Option(academicYear), Option(route), Option(set))

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[Unit],
		@RequestParam(value="updatedMonitoringPoint", required = false) updatedPoint: MonitoringPoint
	) = {
		cmd.apply()
		Mav("home/view", "updatedPoint" -> updatedPoint)
	}

}