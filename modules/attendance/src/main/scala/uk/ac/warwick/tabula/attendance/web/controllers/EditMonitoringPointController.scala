package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.attendance.commands.{EditMonitoringPointState, EditMonitoringPointCommand, AddMonitoringPointCommand}
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint

@Controller
@RequestMapping(Array("/manage/{dept}/sets/add/points/edit/{pointIndex}"))
class EditMonitoringPointController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def createCommand(@PathVariable dept: Department, @PathVariable pointIndex: Int) = EditMonitoringPointCommand(dept, pointIndex)

	@RequestMapping(method=Array(POST), params = Array("form"))
	def form(@ModelAttribute("command") cmd: Appliable[MonitoringPoint] with EditMonitoringPointState) = {
		cmd.copyFrom
		Mav("manage/point/edit_form").noLayoutIf(ajax)
	}

	@RequestMapping(method=Array(POST))
	def submitModal(@Valid @ModelAttribute("command") cmd: Appliable[MonitoringPoint] with EditMonitoringPointState, errors: Errors) = {
		if (errors.hasErrors) {
			Mav("manage/point/edit_form").noLayoutIf(ajax)
		} else {
			cmd.apply()
			Mav("manage/set/_monitoringPoints").noLayoutIf(ajax)
		}
	}

}
