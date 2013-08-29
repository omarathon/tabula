package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.attendance.commands.ListMonitoringPointsCommand
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint

@Controller
@RequestMapping(Array("/monitoringpoints"))
class ListMonitoringPointsController extends AttendanceController {

	@ModelAttribute
	def listMonitoringPointsCommand(@RequestParam(value="page", required = false, defaultValue = "0") page: Int) =
		ListMonitoringPointsCommand(page)

	@RequestMapping(method = Array(GET, HEAD))
	def list(cmd: ListMonitoringPointsCommand, user: CurrentUser,
					 	@RequestParam(value="updatedMonitoringPoint", required = false) updatedMonitoringPoint: MonitoringPoint) = {
		val monitoringPoints = cmd.apply
		Mav("home/list", "monitoringPoints" -> monitoringPoints, "updatedMonitoringPoint" -> updatedMonitoringPoint)
	}

	@RequestMapping(value = Array("/monitoringpoint/{id}/week/{week}/set"), method = Array(GET, HEAD))
	def register(@PathVariable id: String, @PathVariable week: Int) = {
		Mav("home/register", "id" -> id, "week" -> week)
	}


}
