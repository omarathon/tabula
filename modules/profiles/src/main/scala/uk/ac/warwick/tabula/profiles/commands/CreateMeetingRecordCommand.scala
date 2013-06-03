package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.data.model._
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.commands.{NotificationSource, Description}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.Notification
import uk.ac.warwick.tabula.profiles.web.Routes

class CreateMeetingRecordCommand(creator: Member, relationship: StudentRelationship)
	extends ModifyMeetingRecordCommand(creator, relationship) {

	meetingDate = DateTime.now.toLocalDate

	val meeting = new MeetingRecord(creator, relationship)

	override def onBind(result:BindingResult) = transactional() {
		file.onBind(result)
	}

	def describe(d: Description): Unit = {  }
}


