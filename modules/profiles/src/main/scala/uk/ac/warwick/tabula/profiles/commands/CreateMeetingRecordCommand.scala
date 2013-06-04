package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.data.model.{MeetingRecord, StudentRelationship, Member}
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.commands.Description
import org.joda.time.DateTime

class CreateMeetingRecordCommand(creator: Member, relationship: StudentRelationship)
	extends ModifyMeetingRecordCommand(creator, relationship) {

	meetingDate = DateTime.now.toLocalDate

	override def getMeetingRecord: MeetingRecord = {
		new MeetingRecord(creator, relationship)
	}

	override def onBind(result:BindingResult) = transactional() {
		file.onBind(result)
	}

	def describe(d: Description): Unit = {  }
}
