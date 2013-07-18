package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.ItemNotFoundException
import scala.collection.JavaConverters._
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.model.MeetingRecord
import org.springframework.stereotype.Controller
import javax.validation.Valid
import uk.ac.warwick.tabula.web.views.JSONErrorView
import uk.ac.warwick.tabula.profiles.commands.ApproveMeetingRecordCommand
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController

@Controller
@RequestMapping(value = Array("/*/meeting/{meetingRecord}/approval"))
class ApproveMeetingRecordController  extends ProfilesController {
	validatesSelf[ApproveMeetingRecordCommand]

	@ModelAttribute("approveMeetingRecordCommand")
	def getCommand(@PathVariable("meetingRecord") meetingRecord: MeetingRecord) = {
		val approvals = meetingRecord.approvals.asScala
		val approval = approvals.find(_.approver == currentMember).getOrElse{
			throw new ItemNotFoundException
		}
		new ApproveMeetingRecordCommand(approval)
	}

	@RequestMapping(method = Array(POST))
	def approveMeetingRecord(@Valid @ModelAttribute("approveMeetingRecordCommand") command: ApproveMeetingRecordCommand,
		errors: Errors): Mav = {

		val meetingRecordId = command.approval.meetingRecord.id

		if (!errors.hasErrors) {
			val approval = command.apply()
			val resultMap = Map(
				"status" -> "successful"
			)
			Mav(new JSONView(resultMap))
		} else {
			val additionalData = Map("formId" -> "meeting-%s".format(meetingRecordId))
			Mav(new JSONErrorView(errors, additionalData))
		}

	}
}
