package uk.ac.warwick.tabula.profiles.web.controllers.relationships

import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.ItemNotFoundException
import scala.collection.JavaConverters._
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.model.{MeetingRecordApproval, MeetingRecord}
import org.springframework.stereotype.Controller
import javax.validation.Valid
import uk.ac.warwick.tabula.web.views.JSONErrorView
import uk.ac.warwick.tabula.profiles.commands.{ApproveMeetingRecordState, ApproveMeetingRecordCommand}
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}

@Controller
@RequestMapping(value = Array("/*/meeting/{meetingRecord}/approval"))
class ApproveMeetingRecordController  extends ProfilesController {

	validatesSelf[SelfValidating]

	@ModelAttribute("approveMeetingRecordCommand")
	def getCommand(@PathVariable("meetingRecord") meetingRecord: MeetingRecord) = meetingRecord match {
		case meetingRecord: MeetingRecord =>  {
			val approvals = meetingRecord.approvals.asScala
			val approval = approvals.find(_.approver == currentMember).getOrElse{
				throw new ItemNotFoundException
			}
			ApproveMeetingRecordCommand(approval)
		}
		case _ => throw new ItemNotFoundException
	}


	@RequestMapping(method = Array(POST))
	def approveMeetingRecord(@Valid @ModelAttribute("approveMeetingRecordCommand") command: Appliable[MeetingRecordApproval] with ApproveMeetingRecordState,
		errors: Errors): Mav = {

		val meetingRecordId = command.approval.meetingRecord.id

		if (!errors.hasErrors) {
			command.apply()
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
