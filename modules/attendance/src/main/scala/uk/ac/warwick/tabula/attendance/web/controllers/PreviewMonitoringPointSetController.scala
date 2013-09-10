package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable}
import uk.ac.warwick.tabula.data.model.attendance.AbstractMonitoringPointSet
import uk.ac.warwick.tabula.attendance.commands.{ViewMonitoringPointSetState, ViewMonitoringPointSetCommand}
import uk.ac.warwick.tabula.commands.Appliable

/**
 * Render a view of a monitoring point set [template] for displaying
 * inside a modal, as a preview of what the set looks like.
 */
@Controller
class PreviewMonitoringPointSetController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable set: AbstractMonitoringPointSet): Appliable[AbstractMonitoringPointSet] = ViewMonitoringPointSetCommand(set)

	@RequestMapping(Array("/monitoringpoints/preview/{set}"))
	def display(@ModelAttribute("command") cmd: Appliable[AbstractMonitoringPointSet]) =
		Mav("manage/set/preview", "set" -> cmd.apply()).noLayoutIf(ajax)

}

