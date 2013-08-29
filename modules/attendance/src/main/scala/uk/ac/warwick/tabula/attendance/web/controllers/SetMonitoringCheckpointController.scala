package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import scala.Array
import uk.ac.warwick.tabula.attendance.commands.SetMonitoringCheckpointCommand
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.tabula.services.RouteService
import uk.ac.warwick.tabula.web.Mav
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.attendance.web.Routes

@RequestMapping(Array("/monitoringpoints/{monitoringPoint}/week/{week}/set"))
@Controller
class SetMonitoringCheckpointController extends AttendanceController {

	validatesSelf[SetMonitoringCheckpointCommand]

	@Autowired var service: RouteService = _

	@ModelAttribute("command")
	def command(@PathVariable monitoringPoint: MonitoringPoint, @PathVariable week: Int, user: CurrentUser) = {
		SetMonitoringCheckpointCommand(monitoringPoint, week, user)
	}


	@RequestMapping(method = Array(GET, HEAD))
	def list(@ModelAttribute("command") command: SetMonitoringCheckpointCommand): Mav = {
		command.populate()
		form(command)
	}


	def form(@ModelAttribute command: SetMonitoringCheckpointCommand): Mav = {
		Mav("home/set", "command" -> command, "monitoringPoint" -> command.monitoringPoint)
	}


	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") command: SetMonitoringCheckpointCommand, errors: Errors) = {
		if(errors.hasErrors) {
			form(command)
		} else {
			command.apply
			Redirect(Routes.monitoringPoints, "updatedMonitoringPoint" -> command.monitoringPoint.id)
		}
	}

}