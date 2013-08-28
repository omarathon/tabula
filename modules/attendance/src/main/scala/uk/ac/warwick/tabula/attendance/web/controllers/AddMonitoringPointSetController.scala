package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet
import uk.ac.warwick.tabula.attendance.commands.AddMonitoringPointSetCommand
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.{Route, Department}

@Controller
@RequestMapping(Array("/manage/{dept}/sets/add"))
class AddMonitoringPointSetController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def createCommand(@PathVariable dept: Department) = AddMonitoringPointSetCommand(dept)

	@RequestMapping(method=Array(GET,HEAD))
	def form(@PathVariable dept: Department) = {
		// No type param so redirect back
		Redirect("/manage/" + dept.code)
	}

	@RequestMapping(method=Array(GET,HEAD), params=Array("type=blank"))
	def form(@ModelAttribute("command") cmd: Appliable[MonitoringPointSet]) = {
		Mav("manage/set/add_form", "createType" -> "blank")
	}

	@RequestMapping(method=Array(POST))
	def submit(@PathVariable dept: Department, @Valid @ModelAttribute("command") cmd: Appliable[MonitoringPointSet], errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect("/manage/" + dept.code)
		}
	}

}
