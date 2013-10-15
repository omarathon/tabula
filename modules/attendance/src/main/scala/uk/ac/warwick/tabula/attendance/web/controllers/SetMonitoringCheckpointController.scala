package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import scala.Array
import uk.ac.warwick.tabula.attendance.commands.SetMonitoringCheckpointCommand
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.tabula.web.Mav
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model.Department

@RequestMapping(Array("/{department}/{monitoringPoint}/record"))
@Controller
class SetMonitoringCheckpointController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable monitoringPoint: MonitoringPoint, user: CurrentUser) = {
		SetMonitoringCheckpointCommand(mandatory(monitoringPoint), user)
	}


	@RequestMapping(method = Array(GET, HEAD))
	def list(@ModelAttribute("command") command: SetMonitoringCheckpointCommand, @PathVariable department: Department): Mav = {
		command.populate()
		form(command, department)
	}


	def form(@ModelAttribute command: SetMonitoringCheckpointCommand, department: Department): Mav = {
		Mav("home/record",
				"command" -> command,
				"monitoringPoint" -> command.monitoringPoint,
				"returnTo" -> getReturnTo(Routes.department.view(department))
		)
	}


	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") command: SetMonitoringCheckpointCommand, @PathVariable department: Department, errors: Errors) = {
		if(errors.hasErrors) {
			form(command, department)
		} else {
			command.apply()
			Redirect(Routes.department.view(department), "updatedMonitoringPoint" -> command.monitoringPoint.id)
		}
	}

}