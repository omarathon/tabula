package uk.ac.warwick.tabula.web.controllers.attendance.manage.old

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.commands.attendance.manage.old.EditMonitoringPointSetCommand

@Controller
@RequestMapping(Array("/attendance/manage/{dept}/2013/sets/{set}/edit"))
class EditMonitoringPointSetController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def createCommand(@PathVariable set: MonitoringPointSet) =
		EditMonitoringPointSetCommand(set)

	@RequestMapping(method=Array(GET,HEAD))
	def form(@PathVariable dept: Department, @ModelAttribute("command") cmd: Appliable[MonitoringPointSet]) = {
		cmd.apply()
		Mav("attendance/manage/set/edit_form").crumbs(Breadcrumbs.Old.ManagingDepartment(dept))
	}

}
