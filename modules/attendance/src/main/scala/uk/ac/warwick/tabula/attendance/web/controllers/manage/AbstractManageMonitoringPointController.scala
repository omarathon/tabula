package uk.ac.warwick.tabula.attendance.web.controllers.manage

import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model.MeetingFormat
import org.springframework.web.bind.annotation.ModelAttribute

abstract class AbstractManageMonitoringPointController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("allMeetingFormats")
	def allMeetingFormats = MeetingFormat.members

}
