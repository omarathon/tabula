package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.commands.{ComposableCommand, Describable, CommandInternal, SelfValidating, Description}
import uk.ac.warwick.tabula.data.model.{AbstractMeetingRecord, MeetingRecord}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.services.{AutowiringMeetingRecordServiceComponent, MeetingRecordServiceComponent}

trait RemoveMeetingRecordPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: RemoveMeetingRecordState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.MeetingRecord.Delete(meetingRecord.relationship.relationshipType), meetingRecord)
	}
}

trait RemoveMeetingRecordDescription extends Describable[AbstractMeetingRecord] {
	self: RemoveMeetingRecordState =>

	override def describe(d: Description) = d.properties(
		"meetingRecord" -> meetingRecord.id)

}

trait RemoveMeetingRecordValidation {
	self: RemoveMeetingRecordState =>

	def sharedValidation(errors: Errors) {
		if (!meetingRecord.isScheduled && meetingRecord.asInstanceOf[MeetingRecord].isApproved) {
			errors.reject("meetingRecord.delete.approved")
		}
		else if (user.universityId != meetingRecord.creator.universityId) {
			errors.reject("meetingRecord.delete.notOwner")
		}
	}

}

trait RemoveMeetingRecordState {
	def meetingRecord: AbstractMeetingRecord
	def user: CurrentUser
}

class DeleteMeetingRecordCommand(val meetingRecord: AbstractMeetingRecord, val user: CurrentUser)
	extends CommandInternal[AbstractMeetingRecord] {

	self: MeetingRecordServiceComponent =>

	override def applyInternal() = {
		meetingRecord.deleted = true
		meetingRecordService.saveOrUpdate(meetingRecord)
		meetingRecord
	}
}

trait DeleteMeetingRecordCommandValidation extends SelfValidating with RemoveMeetingRecordValidation {
	self: RemoveMeetingRecordState =>

	override def validate(errors: Errors) {
		if (meetingRecord.deleted) errors.reject("meetingRecord.delete.alreadyDeleted")

		sharedValidation(errors)
	}
}

class RestoreMeetingRecordCommand (val meetingRecord: AbstractMeetingRecord, val user: CurrentUser)
	extends CommandInternal[AbstractMeetingRecord] {

	self: MeetingRecordServiceComponent =>

	override def applyInternal() = {
		meetingRecord.deleted = false
		meetingRecordService.saveOrUpdate(meetingRecord)
		meetingRecord
	}
}

trait RestoreMeetingRecordCommandValidation extends SelfValidating with RemoveMeetingRecordValidation {
	self: RemoveMeetingRecordState =>

	override def validate(errors: Errors) {
		if (!meetingRecord.deleted) errors.reject("meetingRecord.delete.notDeleted")

		sharedValidation(errors)
	}
}

class PurgeMeetingRecordCommand (val meetingRecord: AbstractMeetingRecord, val user: CurrentUser)
	extends CommandInternal[AbstractMeetingRecord] {

	self: MeetingRecordServiceComponent =>

	override def applyInternal() = {
		meetingRecordService.purge(meetingRecord)
		meetingRecord
	}
}

object DeleteMeetingRecordCommand {
	def apply(meetingRecord: AbstractMeetingRecord, user: CurrentUser) =
		new DeleteMeetingRecordCommand(meetingRecord, user)
		with ComposableCommand[AbstractMeetingRecord]
		with AutowiringMeetingRecordServiceComponent
		with DeleteMeetingRecordCommandValidation
		with RemoveMeetingRecordPermissions
		with RemoveMeetingRecordDescription
		with RemoveMeetingRecordState
}

object RestoreMeetingRecordCommand {
	def apply(meetingRecord: AbstractMeetingRecord, user: CurrentUser) =
		new RestoreMeetingRecordCommand(meetingRecord, user)
			with ComposableCommand[AbstractMeetingRecord]
			with AutowiringMeetingRecordServiceComponent
			with RestoreMeetingRecordCommandValidation
			with RemoveMeetingRecordPermissions
			with RemoveMeetingRecordDescription
			with RemoveMeetingRecordState
}

object PurgeMeetingRecordCommand {
	def apply(meetingRecord: AbstractMeetingRecord, user: CurrentUser) =
		new PurgeMeetingRecordCommand(meetingRecord, user)
			with ComposableCommand[AbstractMeetingRecord]
			with AutowiringMeetingRecordServiceComponent
			with RestoreMeetingRecordCommandValidation
			with RemoveMeetingRecordPermissions
			with RemoveMeetingRecordDescription
			with RemoveMeetingRecordState
}
