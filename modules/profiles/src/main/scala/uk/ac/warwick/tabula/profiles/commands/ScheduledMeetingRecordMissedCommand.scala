package uk.ac.warwick.tabula.profiles.commands

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.ScheduledMeetingRecord
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringMeetingRecordServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.profiles.notifications.ScheduledMeetingRecordMissedInviteeNotification

object ScheduledMeetingRecordMissedCommand {
	def apply(meetingRecord: ScheduledMeetingRecord) =
		new ScheduledMeetingRecordMissedCommand(meetingRecord)
		with ComposableCommand[ScheduledMeetingRecord]
		with ScheduledMeetingRecordMissedDescription
		with ScheduledMeetingRecordMissedPermission
		with ScheduledMeetingRecordMissedValidation
		with ScheduledMeetingRecordMissedNotification
		with AutowiringMeetingRecordServiceComponent

}

class ScheduledMeetingRecordMissedCommand (val meetingRecord: ScheduledMeetingRecord) extends CommandInternal[ScheduledMeetingRecord] with ScheduledMeetingRecordMissedState {

	self: MeetingRecordServiceComponent =>

	def applyInternal() = {
		meetingRecord.missed = true
		meetingRecord.missedReason = comments
		meetingRecordService.saveOrUpdate(meetingRecord)
		meetingRecord
	}
}

trait ScheduledMeetingRecordMissedValidation extends SelfValidating {
	self: ScheduledMeetingRecordMissedState =>

	def validate(errors: Errors) {
		if (meetingRecord.meetingDate.isAfterNow) {
			errors.reject("meetingRecord.missed.future")
		}
		if (meetingRecord.missed) {
			errors.reject("meetingRecord.missed.already")
		}
	}
}

trait ScheduledMeetingRecordMissedPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ScheduledMeetingRecordMissedState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.MeetingRecord.Update(meetingRecord.relationship.relationshipType), meetingRecord)
	}
}

trait ScheduledMeetingRecordMissedDescription extends Describable[ScheduledMeetingRecord] {
	self: ScheduledMeetingRecordMissedState =>

	override lazy val eventName = "ScheduledMeetingRecordMissed"
	
	def describe(d: Description) {
		d.meeting(meetingRecord)
	}
}

trait ScheduledMeetingRecordMissedNotification extends Notifies[ScheduledMeetingRecord, ScheduledMeetingRecord] {
	self: ScheduledMeetingRecordMissedState =>

	def emit(meeting: ScheduledMeetingRecord) =
		Seq(new ScheduledMeetingRecordMissedInviteeNotification(meeting, "missed"))
}

trait ScheduledMeetingRecordMissedState {
	def meetingRecord: ScheduledMeetingRecord
	var comments: String = _
}
