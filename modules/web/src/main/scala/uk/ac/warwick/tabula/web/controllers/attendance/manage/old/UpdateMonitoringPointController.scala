package uk.ac.warwick.tabula.web.controllers.attendance.manage.old

import javax.validation.Valid

import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.attendance.manage.old.UpdateMonitoringPointCommand
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}

@RequestMapping(Array("/attendance/manage/{dept}/2013/sets/{set}/edit/points/{point}/edit"))
class UpdateMonitoringPointController extends AbstractManageMonitoringPointController {

	@ModelAttribute("command")
	def createCommand(
		@PathVariable set: MonitoringPointSet,
		@PathVariable point: MonitoringPoint
	) =
		UpdateMonitoringPointCommand(set, point)

	@RequestMapping(method=Array(GET, HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[MonitoringPoint]) = {
		Mav("attendance/manage/point/update_form").noLayoutIf(ajax)
	}

	@RequestMapping(method=Array(POST))
	def submitModal(@Valid @ModelAttribute("command") cmd: Appliable[MonitoringPoint], errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Mav("attendance/manage/set/_monitoringPointsPersisted").noLayoutIf(ajax)
		}
	}

}
