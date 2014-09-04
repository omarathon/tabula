package uk.ac.warwick.tabula.profiles.commands

import org.joda.time.DateTime
import org.springframework.validation.BindingResult
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.meetingrecord.NewMeetingRecordApprovalNotification

class CreateMeetingRecordCommand(creator: Member, relationship: StudentRelationship, considerAlternatives: Boolean)
	extends ModifyMeetingRecordCommand(creator, relationship, considerAlternatives) {

	meetingDateTime = DateTime.now.hourOfDay.roundFloorCopy
	isRealTime = true

	val meeting = new MeetingRecord(creator, relationship)

	override def onBind(result:BindingResult) = transactional() {
		file.onBind(result)
	}

	override def emit(meeting: MeetingRecord) = Seq(
		Notification.init(new NewMeetingRecordApprovalNotification, creator.asSsoUser, Seq(meeting), relationship)
	)

	override def scheduledNotifications(result: MeetingRecord) = Seq(
		new ScheduledNotification[MeetingRecord]("newMeetingRecordApproval", result, DateTime.now.plusWeeks(1))
	)
}


