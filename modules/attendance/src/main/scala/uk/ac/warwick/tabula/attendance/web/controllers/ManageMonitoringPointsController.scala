package uk.ac.warwick.tabula.attendance.web.controllers


import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestParam, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Route
import org.joda.time.DateTime
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.attendance.commands.ManageMonitoringPointSetCommand
import uk.ac.warwick.tabula.commands.Appliable

/**
 * Displays the screen for creating and editing monitoring point sets
 */
@Controller
@RequestMapping(Array("/manage/{dept}"))
class ManageMonitoringPointsController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(@PathVariable dept: Department, @RequestParam(value="academicYear", required = false) academicYear: AcademicYear) =
			ManageMonitoringPointSetCommand(dept, Option(academicYear))

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Unit], @RequestParam(value="created", required = false) createdCount: Integer) = {
		cmd.apply()
		Mav("manage/manage", "createdCount" -> createdCount)
	}

}