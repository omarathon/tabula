package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestParam, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}
import uk.ac.warwick.tabula.attendance.commands.{RemoveMonitoringPointCommand, DeleteMonitoringPointCommand}
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.ItemNotFoundException

@Controller
@RequestMapping(Array("/manage/{dept}/points/delete"))
class RemoveMonitoringPointController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def createCommand(@RequestParam set: MonitoringPointSet, @RequestParam point: MonitoringPoint, @PathVariable dept: Department) = {
		if (set.route.department != dept) throw new ItemNotFoundException()
		if (point.pointSet != set) throw new ItemNotFoundException()
		RemoveMonitoringPointCommand(mandatory(set), mandatory(point))
	}

	@RequestMapping(method=Array(GET,HEAD))
	def form(@ModelAttribute("command") cmd: Appliable[Unit]) = {
		Mav("manage/point/delete_form").noLayoutIf(ajax)
	}

	@RequestMapping(method=Array(GET,HEAD), params = Array("modal"))
	def formModal(@ModelAttribute("command") cmd: Appliable[Unit]) = {
		Mav("manage/point/delete_form", "modal" -> true).noLayout
	}

	@RequestMapping(method=Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[Unit], errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect("/manage")
		}
	}

	@RequestMapping(method=Array(POST), params = Array("modal"))
	def submitModal(@Valid @ModelAttribute("command") cmd: Appliable[Unit], errors: Errors) = {
		if (errors.hasErrors) {
			formModal(cmd)
		} else {
			cmd.apply()
			Mav("manage/point/delete_form_success", "modal" -> true).noLayoutIf(ajax)
		}
	}

}
