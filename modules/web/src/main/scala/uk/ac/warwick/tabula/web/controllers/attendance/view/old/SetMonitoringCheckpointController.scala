package uk.ac.warwick.tabula.web.controllers.attendance.view.old

import javax.validation.Valid

import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.attendance.view.old.SetMonitoringCheckpointCommand
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceState, MonitoringPoint}
import uk.ac.warwick.tabula.data.model.{Department, Route}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

@RequestMapping(Array("/attendance/view/{department}/2013/{monitoringPoint}/record"))
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
		Mav("attendance/home/record_point",
			"allCheckpointStates" -> AttendanceState.values,
			"returnTo" -> getReturnTo(Routes.old.department.view(department))
		).crumbs(Breadcrumbs.Old.ViewDepartment(department), Breadcrumbs.Old.ViewDepartmentPoints(department))
	}


	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") command: SetMonitoringCheckpointCommand, errors: Errors,
						 @PathVariable department: Department, @PathVariable monitoringPoint: MonitoringPoint) = {
		if(errors.hasErrors) {
			form(command, department)
		} else {
			command.apply()
			Redirect(Routes.old.department.view(department), "updatedMonitoringPoint" -> monitoringPoint.id)
		}
	}

}