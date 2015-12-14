package uk.ac.warwick.tabula.profiles.web.controllers.relationships

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.model.ScheduledMeetingRecord
import org.springframework.stereotype.Controller
import javax.validation.Valid
import uk.ac.warwick.tabula.web.views.JSONErrorView
import uk.ac.warwick.tabula.commands.profiles.ScheduledMeetingRecordMissedCommand
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}

@Controller
@RequestMapping(value = Array("/*/meeting/{meetingRecord}/missed"))
class ScheduledMeetingRecordMissedController extends ProfilesController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def getCommand(@PathVariable("meetingRecord") meetingRecord: ScheduledMeetingRecord) =
		ScheduledMeetingRecordMissedCommand(mandatory(meetingRecord))


	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") command: Appliable[ScheduledMeetingRecord],
		errors: Errors,
		@PathVariable("meetingRecord") meetingRecord: ScheduledMeetingRecord
	) = {

		if (!errors.hasErrors) {
			command.apply()
			val resultMap = Map(
				"status" -> "successful"
			)
			Mav(new JSONView(resultMap))
		} else {
			val additionalData = Map("formId" -> "meeting-%s".format(meetingRecord.id))
			Mav(new JSONErrorView(errors, additionalData))
		}

	}
}
