package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import scala.Array
import uk.ac.warwick.tabula.attendance.commands.SetMonitoringCheckpointCommand
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpointState, MonitoringPoint}
import uk.ac.warwick.tabula.web.Mav
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model.{Route, Department}
import uk.ac.warwick.tabula.JavaImports._

@RequestMapping(Array("/view/{department}/{monitoringPoint}/record"))
@Controller
class SetMonitoringCheckpointController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable department: Department,
		@PathVariable monitoringPoint: MonitoringPoint,
		user: CurrentUser,
		@RequestParam(value="routes", required=false) routes: JList[Route]
	) = {
		if (routes == null)
			SetMonitoringCheckpointCommand(department, monitoringPoint, user, JArrayList())
		else
			SetMonitoringCheckpointCommand(department, monitoringPoint, user, routes)
	}


	@RequestMapping(method = Array(GET, HEAD))
	def list(@ModelAttribute("command") command: SetMonitoringCheckpointCommand, @PathVariable department: Department): Mav = {
		command.populate()
		form(command, department)
	}


	def form(@ModelAttribute command: SetMonitoringCheckpointCommand, department: Department): Mav = {
		Mav("home/record_point",
			"allCheckpointStates" -> MonitoringCheckpointState.values,
			"returnTo" -> getReturnTo(Routes.department.view(department))
		).crumbs(Breadcrumbs.ViewDepartment(department), Breadcrumbs.ViewDepartmentPoints(department))
	}


	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") command: SetMonitoringCheckpointCommand, errors: Errors,
						 @PathVariable department: Department, @PathVariable monitoringPoint: MonitoringPoint) = {
		if(errors.hasErrors) {
			form(command, department)
		} else {
			command.apply()
			Redirect(Routes.department.view(department), "updatedMonitoringPoint" -> monitoringPoint.id)
		}
	}

}