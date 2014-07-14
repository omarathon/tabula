package uk.ac.warwick.tabula.attendance.web.controllers.agent

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.attendance.commands.{HomeInformation, HomeCommand}
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable

/**
 * Displays the agent home screen, allowing users to choose the relationship type and academic year to view.
 */
@Controller
@RequestMapping(Array("/agent"))
class AgentHomeController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(user: CurrentUser) = HomeCommand(user)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[HomeInformation]) = {
		val info = cmd.apply()

		Mav("agent/home",	"relationshipTypesMap" -> info.relationshipTypesMap)
	}

}