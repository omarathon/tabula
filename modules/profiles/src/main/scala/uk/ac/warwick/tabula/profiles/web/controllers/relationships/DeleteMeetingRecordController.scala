package uk.ac.warwick.tabula.profiles.web.controllers.relationships

import uk.ac.warwick.tabula.commands.profiles.DeleteMeetingRecordCommand
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.data.model.AbstractMeetingRecord
import uk.ac.warwick.tabula.CurrentUser
import org.springframework.validation.Errors
import javax.validation.Valid
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.web.views.JSONErrorView
import uk.ac.warwick.tabula.commands.profiles.RestoreMeetingRecordCommand
import uk.ac.warwick.tabula.commands.profiles.PurgeMeetingRecordCommand
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController

abstract class AbstractRemoveMeetingRecordController extends ProfilesController {

	def doApply(command: Appliable[AbstractMeetingRecord], errors: Errors): Mav = {
		if (!errors.hasErrors) {
			command.apply()
			Mav(new JSONView(Map("status" -> "successful")))
		}
		else {
			Mav(new JSONErrorView(errors))
		}
	}
}

@Controller
@RequestMapping(value = Array("/*/meeting/{meetingRecord}/delete"))
class DeleteMeetingRecordController extends AbstractRemoveMeetingRecordController {

	validatesSelf[SelfValidating]

	@ModelAttribute("deleteMeetingRecordCommand")
	def getDeleteCommand(@PathVariable("meetingRecord") meetingRecord: AbstractMeetingRecord, currentUser: CurrentUser) = {
		DeleteMeetingRecordCommand(meetingRecord, currentUser)
	}

	@RequestMapping(method = Array(POST))
	def deleteMeetingRecord(@Valid @ModelAttribute("deleteMeetingRecordCommand") deleteCommand: Appliable[AbstractMeetingRecord],
			errors: Errors): Mav = {
		doApply(deleteCommand, errors)
	}

}

@Controller
@RequestMapping(value = Array("/*/meeting/{meetingRecord}/restore"))
class RestoreMeetingRecordController extends AbstractRemoveMeetingRecordController {

	showDeletedItems

	validatesSelf[SelfValidating]

	@ModelAttribute("restoreMeetingRecordCommand")
	def getRestoreCommand(@PathVariable("meetingRecord") meetingRecord: AbstractMeetingRecord, currentUser: CurrentUser) = {
		RestoreMeetingRecordCommand(meetingRecord, currentUser)
	}

	@RequestMapping(method = Array(POST))
	def restoreMeetingRecord(@Valid @ModelAttribute("restoreMeetingRecordCommand") restoreCommand: Appliable[AbstractMeetingRecord], errors: Errors): Mav = {
		doApply(restoreCommand, errors)
	}

}

@Controller
@RequestMapping(value = Array("/*/meeting/{meetingRecord}/purge"))
class PurgeMeetingRecordController extends AbstractRemoveMeetingRecordController {

	showDeletedItems

	validatesSelf[SelfValidating]

	@ModelAttribute("purgeMeetingRecordCommand")
	def getPurgeCommand(@PathVariable("meetingRecord") meetingRecord: AbstractMeetingRecord, currentUser: CurrentUser) = {
		PurgeMeetingRecordCommand(meetingRecord, currentUser)
	}

	@RequestMapping(method = Array(POST))
	def purgeMeetingRecord(@Valid @ModelAttribute("purgeMeetingRecordCommand") purgeCommand: Appliable[AbstractMeetingRecord], errors: Errors) = {
		doApply(purgeCommand, errors)
	}

}
