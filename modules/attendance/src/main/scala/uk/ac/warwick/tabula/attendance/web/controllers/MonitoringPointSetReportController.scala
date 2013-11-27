package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet
import uk.ac.warwick.tabula.attendance.commands.MonitoringPointSetReportCommand

@Controller
@RequestMapping(value = Array("/{dept}/{monitoringPointSet}/report"))
class MonitoringPointSetReportController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(@PathVariable dept: Department, @PathVariable monitoringPointSet: MonitoringPointSet) =
		MonitoringPointSetReportCommand(monitoringPointSet)

	@RequestMapping(method = Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: MonitoringPointSetReportCommand) = {
		cmd.populate()
		Mav("home/report").noLayoutIf(ajax)
	}

}
