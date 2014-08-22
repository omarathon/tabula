package uk.ac.warwick.tabula.attendance.web.controllers.profile

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.attendance.commands.profile.{ViewSmallGroupsForPointCommandResult, ViewSmallGroupsForPointCommand, ViewMeetingsForPointCommand}
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import uk.ac.warwick.tabula.data.model.{MeetingFormat, MeetingRecord, StudentMember}

@Controller
@RequestMapping(Array("/profile/{student}/{academicYear}/{point}/groups"))
class ViewSmallGroupsForPointController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(@PathVariable student: StudentMember,	@PathVariable point: AttendanceMonitoringPoint) =
		ViewSmallGroupsForPointCommand(mandatory(student), mandatory(point))

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[ViewSmallGroupsForPointCommandResult]) = {
		Mav("profile/groups", "result" -> cmd.apply()).noLayoutIf(ajax)
	}
}