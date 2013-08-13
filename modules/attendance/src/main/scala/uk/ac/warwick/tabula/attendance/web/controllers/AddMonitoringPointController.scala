package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestParam, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}
import uk.ac.warwick.tabula.attendance.commands.AddMonitoringPointCommand
import uk.ac.warwick.tabula.commands.Appliable
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.ItemNotFoundException

@Controller
@RequestMapping(Array("/manage/{dept}/points/add"))
class AddMonitoringPointController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(@RequestParam set: MonitoringPointSet, @PathVariable dept: Department) = {
		if (set.route.department != dept) throw new ItemNotFoundException()
		AddMonitoringPointCommand(mandatory(set))
	}

	@RequestMapping(method=Array(GET,HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[MonitoringPoint]) = {
		Mav("manage/add/form").noLayoutIf(ajax)
	}

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[MonitoringPoint], errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			val newPoint = cmd.apply()
			Redirect("/manage")
		}
	}

}
