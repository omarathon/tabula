package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.{TestBase, Mockito}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{StudentRelationship, StudentRelationshipType, MeetingRecordApproval, MeetingRecord}
import org.hibernate.Session

class ApproveMeetingRecordCommandTest extends TestBase with Mockito {

	val mockSession = mock[Session]

	@Test
	def testApply {

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val relationship = StudentRelationship("Professor A Tutor", relationshipType, "0123456/1")
		val meetingRecord = new MeetingRecord
		meetingRecord.relationship = relationship
		val proposedApproval = new MeetingRecordApproval
		proposedApproval.meetingRecord = meetingRecord

		meetingRecord.approvals.add(proposedApproval)

		val cmd = new ApproveMeetingRecordCommand(meetingRecord.approvals.get(0)){
			override val session = mockSession
		}

		cmd.approved = true
		var approval = cmd.applyInternal
		approval.meetingRecord.isApproved should be(true)

		cmd.approved = false
		approval = cmd.applyInternal
		approval.meetingRecord.isApproved should be(false)

	}

	@Test
	def validApproval {

		val meetingRecord = new MeetingRecord

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val relationship = StudentRelationship("Professor A Tutor", relationshipType, "0123456/1")
		meetingRecord.relationship = relationship
		val proposedApproval = new MeetingRecordApproval
		proposedApproval.meetingRecord = meetingRecord

		meetingRecord.approvals.add(proposedApproval)

		val cmd = new ApproveMeetingRecordCommand(meetingRecord.approvals.get(0))
		cmd.approved = true

		val errors = new BindException(cmd, "command")


		cmd.validate(errors)
		errors.hasErrors should be(false)
	}

	@Test
	def deletedMeetingRecord {

		val meetingRecord = new MeetingRecord

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val relationship = StudentRelationship("Professor A Tutor", relationshipType, "0123456/1")
		meetingRecord.relationship = relationship
		val proposedApproval = new MeetingRecordApproval
		proposedApproval.meetingRecord = meetingRecord

		meetingRecord.approvals.add(proposedApproval)
		meetingRecord.markDeleted()

		val cmd = new ApproveMeetingRecordCommand(meetingRecord.approvals.get(0))
		cmd.approved = true

		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getGlobalError.getCode should be ("meetingRecordApproval.approve.deleted")

	}

}
