package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.{TestBase, Mockito}
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{StudentRelationship, StudentRelationshipType, MeetingRecordApproval, MeetingRecord}
import uk.ac.warwick.tabula.data.{MeetingRecordDaoComponent, MeetingRecordDao}
import uk.ac.warwick.tabula.services.{MonitoringPointMeetingRelationshipTermService, MonitoringPointMeetingRelationshipTermServiceComponent}

class ApproveMeetingRecordCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends ApproveMeetingRecordState with MeetingRecordDaoComponent with ApproveMeetingRecordValidation
		with MonitoringPointMeetingRelationshipTermServiceComponent {
		val meetingRecordDao = mock[MeetingRecordDao]
		val monitoringPointMeetingRelationshipTermService = mock[MonitoringPointMeetingRelationshipTermService]
	}

	trait Fixture {
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val relationship = StudentRelationship("Professor A Tutor", relationshipType, "0123456/1")
		val meetingRecord = new MeetingRecord
		meetingRecord.relationship = relationship
		val proposedApproval = new MeetingRecordApproval
		proposedApproval.meetingRecord = meetingRecord

		meetingRecord.approvals.add(proposedApproval)

		val cmd = new ApproveMeetingRecordCommand(meetingRecord.approvals.get(0)) with CommandTestSupport
	}

	@Test
	def testApplyTrue() {
		new Fixture {
			cmd.approved = true
			val approval = cmd.applyInternal()
			approval.meetingRecord.isApproved should be(true)
			there was one(cmd.meetingRecordDao).saveOrUpdate(approval)
			there was one(cmd.monitoringPointMeetingRelationshipTermService).updateCheckpointsForMeeting(approval.meetingRecord)
		}
	}

	@Test
	def testApplyFalse() {
		new Fixture {
			cmd.approved = false
			val approval = cmd.applyInternal()
			approval.meetingRecord.isApproved should be(false)
			there was one(cmd.meetingRecordDao).saveOrUpdate(approval)
			there was one(cmd.monitoringPointMeetingRelationshipTermService).updateCheckpointsForMeeting(approval.meetingRecord)
		}
	}

	@Test
	def validApproval() {

		val meetingRecord = new MeetingRecord

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val relationship = StudentRelationship("Professor A Tutor", relationshipType, "0123456/1")
		meetingRecord.relationship = relationship
		val proposedApproval = new MeetingRecordApproval
		proposedApproval.meetingRecord = meetingRecord

		meetingRecord.approvals.add(proposedApproval)

		val cmd = new ApproveMeetingRecordCommand(meetingRecord.approvals.get(0)) with CommandTestSupport
		cmd.approved = true

		val errors = new BindException(cmd, "command")


		cmd.validate(errors)
		errors.hasErrors should be(false)
	}

	@Test
	def deletedMeetingRecord() {

		val meetingRecord = new MeetingRecord

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val relationship = StudentRelationship("Professor A Tutor", relationshipType, "0123456/1")
		meetingRecord.relationship = relationship
		val proposedApproval = new MeetingRecordApproval
		proposedApproval.meetingRecord = meetingRecord

		meetingRecord.approvals.add(proposedApproval)
		meetingRecord.markDeleted()

		val cmd = new ApproveMeetingRecordCommand(meetingRecord.approvals.get(0)) with CommandTestSupport
		cmd.approved = true

		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getGlobalError.getCode should be ("meetingRecordApproval.meetingRecord.deleted")

	}

}
