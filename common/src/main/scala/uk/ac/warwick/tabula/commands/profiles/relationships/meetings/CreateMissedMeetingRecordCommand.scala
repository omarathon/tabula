package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.notifications.profiles.meetingrecord.{MissedMeetingRecordAgentNotification, MissedMeetingRecordStudentNotification}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringMeetingRecordServiceComponent, AutowiringAttendanceMonitoringMeetingRecordServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringFileAttachmentServiceComponent, AutowiringMeetingRecordServiceComponent, FileAttachmentServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}

import scala.collection.JavaConverters._

object CreateMissedMeetingRecordCommand {
	def apply(creator: Member, relationships: Seq[StudentRelationship]) =
		new CreateMissedMeetingRecordCommandInternal(creator, relationships)
			with AutowiringMeetingRecordServiceComponent
			with AutowiringFeaturesComponent
			with AutowiringAttendanceMonitoringMeetingRecordServiceComponent
			with AutowiringFileAttachmentServiceComponent
			with ComposableCommand[MeetingRecord]
			with MeetingRecordCommandBindListener
			with ModifyMeetingRecordValidation
			with CreateMeetingRecordDescription
			with ModifyMeetingRecordPermissions
			with CreateMissedMeetingRecordCommandState
			with MissedMeetingRecordCommandRequest
			with CreateMissedMeetingRecordCommandNotifications
			with PopulateOnForm {
			override def populate(): Unit = {}
		}
}

class CreateMissedMeetingRecordCommandInternal(val creator: Member, val allRelationships: Seq[StudentRelationship])
	extends AbstractModifyMeetingRecordCommand {

	self: CreateMeetingRecordCommandState with MissedMeetingRecordCommandRequest with MeetingRecordServiceComponent
		with FeaturesComponent with AttendanceMonitoringMeetingRecordServiceComponent
		with FileAttachmentServiceComponent =>

	override def applyInternal(): MeetingRecord = {
		val meeting = new MeetingRecord(creator, relationships.asScala)
		meeting.missed = true
		meeting.missedReason = missedReason
		applyCommon(meeting)
	}

}

trait CreateMissedMeetingRecordCommandState extends CreateMeetingRecordCommandState {
	override def missed: Boolean = true
}

trait CreateMissedMeetingRecordCommandNotifications extends Notifies[MeetingRecord, MeetingRecord] {

	self: CreateMeetingRecordCommandState =>

	override def emit(meeting: MeetingRecord) = Seq(
		Notification.init(new MissedMeetingRecordStudentNotification, creator.asSsoUser, Seq(meeting)),
		Notification.init(new MissedMeetingRecordAgentNotification, creator.asSsoUser, Seq(meeting))
	)
}

trait MissedMeetingRecordCommandRequest extends MeetingRecordCommandRequest {
	var missedReason: String = _
}
