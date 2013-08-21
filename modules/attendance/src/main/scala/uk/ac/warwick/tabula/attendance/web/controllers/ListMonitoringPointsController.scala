package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestParam, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.attendance.commands.ListMonitoringPointsCommand

@Controller
@RequestMapping(Array("/list"))
class ListMonitoringPointsController extends BaseController {

	@ModelAttribute
	def listMonitoringPointsCommand(@RequestParam(value="page", required = false, defaultValue = "0") page: Int) =
		ListMonitoringPointsCommand(page)

	@RequestMapping(method = Array(GET, HEAD))
	def list(cmd: ListMonitoringPointsCommand, user: CurrentUser) = {
		val monitoringPoints = cmd.apply
		Mav("home/list", "monitoringPoints" -> monitoringPoints)
	}


}
