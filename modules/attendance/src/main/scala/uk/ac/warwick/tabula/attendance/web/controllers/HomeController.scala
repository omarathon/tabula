package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.attendance.commands.{HomeCommand, ManageHomeCommand}
import uk.ac.warwick.tabula.commands.Appliable

/**
 * Displays the Attendance home screen, allowing users to choose the department to view or manage.
 * If the user only has permissions over a single department, they are taken directly to it.
 */
@Controller
@RequestMapping(Array("/"))
class HomeController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(user: CurrentUser) = HomeCommand(user)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Map[String, Set[Department]]]) = {
		val map = cmd.apply()
		if (map("Manage").size == 0 && map("View").size == 1) {
			Redirect(s"/${map("View").head.code}")
		} else {
			Mav("home/home", "permissionMap" -> map)
		}
	}

}