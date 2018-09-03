package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.NewMeetingRecordApprovalNotification
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringMeetingRecordServiceComponent, AutowiringAttendanceMonitoringMeetingRecordServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringMeetingRecordServiceComponent, FileAttachmentServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}

import scala.collection.JavaConverters._

object CreateMeetingRecordCommand {
	def apply(creator: Member, allRelationships: Seq[StudentRelationship]) =
		new CreateMeetingRecordCommandInternal(creator, allRelationships)
			with AutowiringMeetingRecordServiceComponent
			with AutowiringFeaturesComponent
			with AutowiringAttendanceMonitoringMeetingRecordServiceComponent
			with AutowiringFileAttachmentServiceComponent
			with ComposableCommand[MeetingRecord]
			with MeetingRecordCommandBindListener
			with ModifyMeetingRecordValidation
			with CreateMeetingRecordDescription
			with ModifyMeetingRecordPermissions
			with CreateMeetingRecordCommandState
			with MeetingRecordCommandRequest
			with CreateMeetingRecordCommandNotifications
			with PopulateOnForm {
			override def populate(): Unit = {}
		}
}


class CreateMeetingRecordCommandInternal(val creator: Member, val allRelationships: Seq[StudentRelationship])
	extends AbstractModifyMeetingRecordCommand {

	self: CreateMeetingRecordCommandState with MeetingRecordCommandRequest with MeetingRecordServiceComponent
		with FeaturesComponent with AttendanceMonitoringMeetingRecordServiceComponent
		with FileAttachmentServiceComponent =>

	override def applyInternal(): MeetingRecord = {
		val meeting = new MeetingRecord(creator, relationships.asScala)
		applyCommon(meeting)
	}

}

trait CreateMeetingRecordDescription extends ModifyMeetingRecordDescription {

	self: ModifyMeetingRecordCommandState with MeetingRecordCommandRequest =>

	override lazy val eventName = "CreateMeetingRecord"

}

trait CreateMeetingRecordCommandState extends ModifyMeetingRecordCommandState {
	override def creator: Member
}

trait CreateMeetingRecordCommandNotifications extends Notifies[MeetingRecord, MeetingRecord]
	with SchedulesNotifications[MeetingRecord, MeetingRecord] {

	self: CreateMeetingRecordCommandState =>

	override def emit(meeting: MeetingRecord) = Seq(
		Notification.init(new NewMeetingRecordApprovalNotification, creator.asSsoUser, Seq(meeting))
	)

	override def transformResult(meetingRecord: MeetingRecord) = Seq(meetingRecord)

	override def scheduledNotifications(result: MeetingRecord) = Seq(
		new ScheduledNotification[MeetingRecord]("newMeetingRecordApproval", result, DateTime.now.plusWeeks(1))
	)

}