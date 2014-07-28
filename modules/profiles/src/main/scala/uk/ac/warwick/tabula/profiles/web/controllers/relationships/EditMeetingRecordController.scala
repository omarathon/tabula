package uk.ac.warwick.tabula.profiles.web.controllers.relationships

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.profiles.commands.EditMeetingRecordCommand
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.services.AutowiringMonitoringPointMeetingRelationshipTermServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.AutowiringAttendanceMonitoringMeetingRecordServiceComponent

@Controller
@RequestMapping(value = Array("/{relationshipType}/meeting/{studentCourseDetails}/edit/{meetingRecord}"))
class EditMeetingRecordController extends ProfilesController
	with MeetingRecordModal with AutowiringMonitoringPointMeetingRelationshipTermServiceComponent
	with AutowiringAttendanceMonitoringMeetingRecordServiceComponent{

	validatesSelf[EditMeetingRecordCommand]

	@ModelAttribute("command")
	def getCommand(@PathVariable("meetingRecord") meetingRecord: MeetingRecord) =
		new EditMeetingRecordCommand(mandatory(meetingRecord))
}
