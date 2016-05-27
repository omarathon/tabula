package uk.ac.warwick.tabula.web.controllers.profiles.relationships.meetings

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.profiles.relationships.meetings.ApproveMeetingRecordCommand
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{MeetingRecord, MeetingRecordApproval}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

@Controller
@RequestMapping(value = Array("/profiles/*/meeting/{meetingRecord}/approval"))
class ApproveMeetingRecordController  extends ProfilesController {

	validatesSelf[SelfValidating]

	@ModelAttribute("approveMeetingRecordCommand")
	def getCommand(@PathVariable meetingRecord: MeetingRecord) =
		ApproveMeetingRecordCommand(mandatory(meetingRecord), user)


	@RequestMapping(method = Array(POST))
	def approveMeetingRecord(
		@Valid @ModelAttribute("approveMeetingRecordCommand") command: Appliable[MeetingRecordApproval],
		errors: Errors,
		@PathVariable meetingRecord: MeetingRecord
	): Mav = {

		val meetingRecordId = meetingRecord.id

		if (!errors.hasErrors) {
			command.apply()
			Mav(new JSONView(Map(
				"status" -> "successful"
			)))
		} else {
			Mav(new JSONErrorView(errors, Map("formId" -> "meeting-%s".format(meetingRecordId))))
		}

	}
}
