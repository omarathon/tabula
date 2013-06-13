package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.profiles.commands.EditMeetingRecordCommand
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController

@Controller
@RequestMapping(value = Array("/tutor/meeting/{studentCourseDetails}/edit/{meetingRecord}"))
class EditMeetingRecordController extends ProfilesController with MeetingRecordModal {

	validatesSelf[EditMeetingRecordCommand]

	@ModelAttribute("command")
	def getCommand(@PathVariable("meetingRecord") meetingRecord: MeetingRecord) =
		new EditMeetingRecordCommand(meetingRecord)
}
