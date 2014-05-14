package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.attendance.commands.manage.{EditSchemeMembershipCommand, EditSchemeMembershipCommandResult}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/{scheme}/students/membership"))
class EditSchemeMembershipController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable scheme: AttendanceMonitoringScheme) =
		EditSchemeMembershipCommand(scheme)

	@RequestMapping(method = Array(POST))
	def submit(@ModelAttribute("command") cmd: Appliable[EditSchemeMembershipCommandResult]) = {
		val result = cmd.apply()
		Mav("manage/studentstable",
			"membershipItems" -> result.membershipItems,
			// FIXME this is wrong
			"memberCount" -> result.membershipItems.size,
			"missingMembers" -> result.missingMembers
		).noLayout()
	}

}
