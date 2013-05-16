package uk.ac.warwick.tabula.profiles.commands
import scala.collection.JavaConverters._
import org.springframework.validation.Errors
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.MeetingRecordDao
import uk.ac.warwick.tabula.data.model.MeetingApprovalState._
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.data.model.MeetingRecordApproval
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.Daoisms
import org.joda.time.DateTime

class ApproveMeetingRecordCommand (val meetingRecord: MeetingRecord, val currentMember: Member)
	extends Command[MeetingRecordApproval] with SelfValidating with Daoisms {
	PermissionCheck(Permissions.Profiles.MeetingRecord.Update, meetingRecord)

	var meetingRecordDao = Wire.auto[MeetingRecordDao]

	var approved: JBoolean = _
	var rejectionComments: String =_

	def validate(errors: Errors) {
		if (approved == null) {
			errors.rejectValue("approved", "meetingRecordApproval.approved.isNull")
		}
		else if (!approved && !rejectionComments.hasText) {
			errors.rejectValue("rejectionComments", "meetingRecordApproval.rejectionComments.isEmpty")
		}
	}

	def applyInternal() = {
		val approvals = meetingRecord.approvals.asScala
		val approval = approvals.find(_.approver == currentMember).getOrElse{
			throw new ItemNotFoundException
		}

		if (approved) {
			approval.state = Approved
		}
		else {
			approval.state = Rejected
			approval.comments = rejectionComments
		}

		approval.lastUpdatedDate = DateTime.now

		session.saveOrUpdate(approval)

		approval
	}

	def describe(d: Description) = d.properties(
		"meetingRecord" -> meetingRecord.id)
}
