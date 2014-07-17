package uk.ac.warwick.tabula.attendance.web.controllers.profile

import uk.ac.warwick.tabula.data.model.{MeetingFormat, MeetingRecord, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringPoint
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, ModelAttribute}
import uk.ac.warwick.tabula.commands.Appliable
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.attendance.commands.profile.ViewMeetingsForPointCommand

@Controller
@RequestMapping(Array("/profile/{student}/{academicYear}/{point}/meetings"))
class ViewMeetingsForPointController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(@PathVariable student: StudentMember,	@PathVariable point: AttendanceMonitoringPoint) =
		ViewMeetingsForPointCommand(mandatory(student), mandatory(point))

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Seq[Pair[MeetingRecord, Seq[String]]]]) = {
		val meetingsStatuses = cmd.apply()
		Mav("home/meetings",
			"meetingsStatuses" -> meetingsStatuses,
			"allMeetingFormats" -> MeetingFormat.members
		).noLayoutIf(ajax)
	}
}