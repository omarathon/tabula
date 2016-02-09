package uk.ac.warwick.tabula.web.controllers.attendance.view

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.attendance.{HomeInformation, HomeCommand}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable

/**
 * Displays the view home screen, allowing users to choose the department and academic year to view.
 */
@Controller
@RequestMapping(Array("/attendance/view"))
class ViewHomeController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(user: CurrentUser) = HomeCommand(user)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[HomeInformation]) = {
		val info = cmd.apply()

		Mav("attendance/view/home",	"viewPermissions" -> info.viewPermissions)
	}

}