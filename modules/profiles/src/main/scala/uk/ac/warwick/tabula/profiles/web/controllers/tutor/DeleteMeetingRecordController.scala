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
import uk.ac.warwick.tabula.profiles.commands.RestoreMeetingRecordCommand
import uk.ac.warwick.tabula.profiles.commands.PurgeMeetingRecordCommand
import uk.ac.warwick.tabula.profiles.commands.AbstractDeleteMeetingRecordCommand
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(value = Array("/tutor/meeting/{meetingRecord}"))
class DeleteMeetingRecordController  extends BaseController {
	validatesSelf[DeleteMeetingRecordCommand]

	@ModelAttribute("deleteMeetingRecordCommand")
	def getDeleteCommand(@PathVariable("meetingRecord") meetingRecord: MeetingRecord, currentUser: CurrentUser) = {
		new DeleteMeetingRecordCommand(meetingRecord, currentUser)
	}

	@ModelAttribute("restoreMeetingRecordCommand")
	def getRestoreCommand(@PathVariable("meetingRecord") meetingRecord: MeetingRecord, currentUser: CurrentUser) = {
		new RestoreMeetingRecordCommand(meetingRecord, currentUser)
	}

	@ModelAttribute("purgeMeetingRecordCommand")
	def getPurgeCommand(@PathVariable("meetingRecord") meetingRecord: MeetingRecord, currentUser: CurrentUser) = {
		new PurgeMeetingRecordCommand(meetingRecord, currentUser)
	}

	@RequestMapping(value = Array("delete"), method = Array(POST))
	def deleteMeetingRecord(@Valid @ModelAttribute("deleteMeetingRecordCommand") deleteCommand: DeleteMeetingRecordCommand,
			errors: Errors): Mav = {
		doApply(deleteCommand, errors)
	}

	@RequestMapping(value = Array("restore"), method = Array(POST))
	def restoreMeetingRecord(@Valid @ModelAttribute("restoreMeetingRecordCommand") restoreCommand: RestoreMeetingRecordCommand,
			errors: Errors): Mav = {
		doApply(restoreCommand, errors)
	}

	@RequestMapping(value = Array("purge"), method = Array(POST))
	def purgeMeetingRecord(@Valid @ModelAttribute("purgeMeetingRecordCommand") purgeCommand: PurgeMeetingRecordCommand,
			errors: Errors): Mav = {
		doApply(purgeCommand, errors)
	}

	def doApply(command: AbstractDeleteMeetingRecordCommand[_], errors: Errors): Mav = {
		transactional() {
			if (!errors.hasErrors) {
				command.apply()
				Mav(new JSONView(Map("status" -> "successful")))
			}
			else {
				Mav(new JSONErrorView(errors))
			}
		}
	}
}
