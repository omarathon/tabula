package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.attendance.commands.ManageHomeCommand
import uk.ac.warwick.tabula.commands.Appliable

/**
 * Displays the managing home screen, allowing users to choose the department to manage.
 * If the user only has permissions over a single department, they are taken directly to it.
 */
@Controller
@RequestMapping(Array("/manage"))
class ManageMonitoringPointsController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(user: CurrentUser) = ManageHomeCommand(user)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Set[Department]]) = {
		val departments = cmd.apply()
		if (departments.size == 1)
			Redirect(s"/manage/${departments.head.code}")
		else
			Mav("manage/home", "departments" -> departments)
	}

}