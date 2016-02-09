package uk.ac.warwick.tabula.web.controllers.attendance

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.commands.attendance.old.{ViewMonitoringPointSetCommand, ViewMonitoringPointSetTemplateCommand}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPointSetTemplate}

/**
 * Render a view of a monitoring point set [template] for displaying
 * inside a modal, as a preview of what the set looks like.
 */
@Controller
class PreviewMonitoringPointSetController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable set: MonitoringPointSet): Appliable[MonitoringPointSet] = ViewMonitoringPointSetCommand(mandatory(set))

	@RequestMapping(Array("/attendance/monitoringpoints/preview/{set}"))
	def display(@ModelAttribute("command") cmd: Appliable[MonitoringPointSet]) =
		Mav("attendance/manage/set/preview", "set" -> cmd.apply()).noLayoutIf(ajax)

}

@Controller
class PreviewMonitoringPointSetTemplateController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable set: MonitoringPointSetTemplate): Appliable[MonitoringPointSetTemplate] = ViewMonitoringPointSetTemplateCommand(mandatory(set))

	@RequestMapping(Array("/attendance/monitoringpoints/preview/template/{set}"))
	def display(@ModelAttribute("command") cmd: Appliable[MonitoringPointSetTemplate]) =
		Mav("attendance/manage/set/preview", "set" -> cmd.apply()).noLayoutIf(ajax)

}
