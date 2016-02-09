package uk.ac.warwick.tabula.web.controllers.attendance.manage.old

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestParam, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSetTemplate
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.commands.attendance.manage.old.ManageMonitoringPointSetCommand

/**
 * Displays the screen for creating and editing monitoring point sets
 */
@Controller
@RequestMapping(Array("/attendance/manage/{dept}/2013"))
class ManageMonitoringPointsForDeptController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(@PathVariable dept: Department) =
			ManageMonitoringPointSetCommand(user, dept, Option(AcademicYear(2013)))

	@RequestMapping
	def home(
		@ModelAttribute("command") cmd: Appliable[Seq[MonitoringPointSetTemplate]],
		@RequestParam(value="created", required = false) createdCount: Integer
	) = {
		val templates = cmd.apply()
		Mav("attendance/manage/manage", "templates" -> templates, "createdCount" -> createdCount)
	}

}