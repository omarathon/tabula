package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.profiles.commands.EditMeetingRecordCommand

@Controller
@RequestMapping(value = Array("/tutor/meeting/{student}/edit/{meetingRecord}"))
class EditMeetingRecordController extends MeetingRecordModal {

	validatesSelf[EditMeetingRecordCommand]

	@ModelAttribute("command")
	def getCommand(@PathVariable("meetingRecord") meetingRecord: MeetingRecord) =
		new EditMeetingRecordCommand(meetingRecord)
}
