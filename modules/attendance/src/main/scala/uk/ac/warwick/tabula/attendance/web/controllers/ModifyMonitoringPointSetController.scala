package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.{Route, Department}
import uk.ac.warwick.tabula.web.views.JSONView
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.attendance.commands.ModifyMonitoringPointSetCommand

@Controller
@RequestMapping(Array("/manage/{dept}/sets/edit"))
class ModifyMonitoringPointSetController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def modifyCommand(@RequestParam route: Route, @RequestParam set: MonitoringPointSet) =
		ModifyMonitoringPointSetCommand(route, set)

	@RequestMapping(method=Array(POST))
	def submit(@PathVariable dept: Department, @Valid @ModelAttribute("command") cmd: Appliable[MonitoringPointSet], errors: Errors) = {
		if (errors.hasErrors) {
			new JSONView(Map(
				"errors" -> errors.getFieldErrors.asScala.groupBy(f => f.getField).mapValues(fieldErrors => fieldErrors.map(f => getMessage(f.getCode)))
			))
		} else {
			val set = cmd.apply()
			new JSONView(Map(
				"errors" -> Array(),
				"monitoringPointSet" -> Map(
					"id" -> set.id,
					"templateName" -> set.templateName
				)
			))
		}
	}

}
