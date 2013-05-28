package uk.ac.warwick.tabula.profiles.commands

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model.MeetingApprovalState._
import uk.ac.warwick.tabula.data.model.MeetingRecordApproval
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.Daoisms
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.Transactions._

class ApproveMeetingRecordCommand (val approval: MeetingRecordApproval)
	extends Command[MeetingRecordApproval] with SelfValidating with Daoisms {

	PermissionCheck(Permissions.Profiles.MeetingRecord.Update, approval.meetingRecord)

	var approved: JBoolean = _
	var rejectionComments: String =_

	def validate(errors: Errors) {
		if (approved == null) {
			errors.rejectValue("approved", "meetingRecordApproval.approved.isNull")
		} else if (!approved && !rejectionComments.hasText) {
			errors.rejectValue("rejectionComments", "meetingRecordApproval.rejectionComments.isEmpty")
		}
	}

	def applyInternal() = transactional() {
		if (approved) {
			approval.state = Approved
		} else {
			approval.state = Rejected
			approval.comments = rejectionComments
		}

		approval.lastUpdatedDate = DateTime.now

		session.saveOrUpdate(approval)

		approval
	}

	def describe(d: Description) = d.properties(
		"meetingRecord" -> approval.meetingRecord.id)
}
