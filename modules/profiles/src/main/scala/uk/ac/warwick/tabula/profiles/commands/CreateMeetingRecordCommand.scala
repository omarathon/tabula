package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.data.model._
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.commands.Description
import org.joda.time.DateTime
import uk.ac.warwick.tabula.profiles.notifications.MeetingRecordApprovalNotification

class CreateMeetingRecordCommand(creator: Member, relationship: StudentRelationship, considerAlternatives: Boolean)
	extends ModifyMeetingRecordCommand(creator, relationship, considerAlternatives) {

	meetingDate = DateTime.now.toLocalDate

	val meeting = new MeetingRecord(creator, relationship)

	override def onBind(result:BindingResult) = transactional() {
		file.onBind(result)
	}

	def emit(meeting: MeetingRecord) = Seq(new MeetingRecordApprovalNotification(meeting, "create"))
}


