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

class DeleteMeetingRecordCommand (val meetingRecord: MeetingRecord, user: CurrentUser) extends Command[MeetingRecord] with SelfValidating {

	PermissionCheck(Permissions.Profiles.MeetingRecord.Delete, meetingRecord.relationship.studentMember)

	var meetingRecordDao = Wire.auto[MeetingRecordDao]


	def applyInternal(): MeetingRecord = {
		meetingRecord.deleted = true
		meetingRecordDao.saveOrUpdate(meetingRecord)
		meetingRecord
	}

	def validate(errors: Errors) {
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
