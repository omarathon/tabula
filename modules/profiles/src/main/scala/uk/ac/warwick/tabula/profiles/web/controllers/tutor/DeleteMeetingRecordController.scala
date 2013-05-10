package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import uk.ac.warwick.tabula.profiles.commands.DeleteMeetingRecordCommand
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.CurrentUser
import org.springframework.validation.Errors
import javax.validation.Valid
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.web.views.JSONErrorView

@Controller
@RequestMapping(value = Array("/tutor/meeting/{meetingRecord}/delete"))
class DeleteMeetingRecordController  extends BaseController {
	validatesSelf[DeleteMeetingRecordCommand]

	@ModelAttribute("deleteMeetingRecordCommand")
	def getCommand(@PathVariable("meetingRecord") meetingRecord: MeetingRecord, currentUser: CurrentUser) = {
		new DeleteMeetingRecordCommand(meetingRecord, currentUser)
	}

	@RequestMapping(method = Array(POST))
	def deleteMeetingRecord(@Valid @ModelAttribute("deleteMeetingRecordCommand") deleteCommand: DeleteMeetingRecordCommand,
			errors: Errors,
			@PathVariable("meetingRecord") meetingRecord: MeetingRecord) = {
		transactional() {
			if (!errors.hasErrors) {
				val meetingRecord = deleteCommand.apply()
				Mav(new JSONView(Map("status" -> "successful")))
			}
			else {
				Mav(new JSONErrorView(errors))
			}
		}
	}
}
