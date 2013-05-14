package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.commands.Description
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.MeetingRecordDao
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.Daoisms

abstract class AbstractDeleteMeetingRecordCommand[A] (val meetingRecord: MeetingRecord, val user: CurrentUser) extends Command[A] with SelfValidating {

	PermissionCheck(Permissions.Profiles.MeetingRecord.Delete, meetingRecord)

	var meetingRecordDao = Wire.auto[MeetingRecordDao]

	def contextSpecificValidation(error:Errors)

	def validate(errors: Errors) {
		contextSpecificValidation(errors)
		if (meetingRecord.isApproved) {
			errors.reject("meetingRecord.delete.approved")
		}
		else if (user.universityId != meetingRecord.creator.universityId) {
			errors.reject("meetingRecord.delete.notOwner")
		}
	}

	def describe(d: Description) = d.properties(
		"meetingRecord" -> meetingRecord.id)
}

class DeleteMeetingRecordCommand(meetingRecord: MeetingRecord, user: CurrentUser)
	extends AbstractDeleteMeetingRecordCommand[MeetingRecord](meetingRecord, user) {

	override def contextSpecificValidation(errors: Errors) {
		if (meetingRecord.deleted) errors.reject("meetingRecord.delete.alreadyDeleted")
	}

	override def applyInternal() = {
		meetingRecord.deleted = true
		meetingRecordDao.saveOrUpdate(meetingRecord)
		meetingRecord
	}
}

class RestoreMeetingRecordCommand (meetingRecord: MeetingRecord, user: CurrentUser)
	extends AbstractDeleteMeetingRecordCommand[MeetingRecord](meetingRecord, user) {

	override def contextSpecificValidation(errors: Errors) {
		if (!meetingRecord.deleted) errors.reject("meetingRecord.delete.notDeleted")
	}

	override def applyInternal() = {
		meetingRecord.deleted = false
		meetingRecordDao.saveOrUpdate(meetingRecord)
		meetingRecord
	}
}

class PurgeMeetingRecordCommand (meetingRecord: MeetingRecord, user: CurrentUser)
	extends AbstractDeleteMeetingRecordCommand[Unit](meetingRecord, user) with Daoisms {

	override def contextSpecificValidation(errors: Errors) {
		if (!meetingRecord.deleted) errors.reject("meetingRecord.delete.notDeleted")
	}

	override def applyInternal() = {
		session.delete(meetingRecord)
		session.flush
	}
}
