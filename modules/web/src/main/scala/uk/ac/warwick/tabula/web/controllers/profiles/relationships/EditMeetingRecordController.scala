package uk.ac.warwick.tabula.web.controllers.profiles.relationships

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.profiles.EditMeetingRecordCommand
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.services.AutowiringTermServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

@Controller
@RequestMapping(value = Array("/profiles/{relationshipType}/meeting/{studentCourseDetails}/edit/{meetingRecord}"))
class EditMeetingRecordController extends ProfilesController
	with MeetingRecordModal
	with AutowiringAttendanceMonitoringMeetingRecordServiceComponent
	with AutowiringTermServiceComponent {

	validatesSelf[EditMeetingRecordCommand]

	@ModelAttribute("command")
	def getCommand(@PathVariable meetingRecord: MeetingRecord) =
		new EditMeetingRecordCommand(mandatory(meetingRecord))
}
