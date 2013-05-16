package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.model.MeetingRecord
import org.springframework.stereotype.Controller
import javax.validation.Valid
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.web.views.JSONErrorView
import uk.ac.warwick.tabula.profiles.commands.ApproveMeetingRecordCommand
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.data.model.Member

@Controller
@RequestMapping(value = Array("/tutor/meeting/{meetingRecord}/approval"))
class ApproveMeetingRecordController  extends ProfilesController {
	validatesSelf[ApproveMeetingRecordCommand]

	@ModelAttribute("approveMeetingRecordCommand")
	def getCommand(@PathVariable("meetingRecord") meetingRecord: MeetingRecord) = {
		new ApproveMeetingRecordCommand(meetingRecord, currentMember)
	}

	@RequestMapping(method = Array(POST))
	def approveMeetingRecord(@Valid @ModelAttribute("approveMeetingRecordCommand") command: ApproveMeetingRecordCommand,
			errors: Errors): Mav = {

		transactional() {
			val meetingRecordId = command.meetingRecord.id

			if (!errors.hasErrors) {
				val approval = command.apply()
				val resultMap = Map(
					"status" -> "successful"
				)
				Mav(new JSONView(resultMap))
			}
			else {
				val additionalData = Map("formId" -> "meeting-%s".format(meetingRecordId))
				Mav(new JSONErrorView(errors, additionalData))
			}
		}
	}
}
