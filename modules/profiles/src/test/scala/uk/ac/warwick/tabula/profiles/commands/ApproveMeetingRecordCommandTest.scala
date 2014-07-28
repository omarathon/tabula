package uk.ac.warwick.tabula.profiles.commands

import org.springframework.validation.BindException
import uk.ac.warwick.tabula.data.model.{ExternalStudentRelationship, MeetingRecord, MeetingRecordApproval, StudentRelationshipType}
import uk.ac.warwick.tabula.data.{MeetingRecordDao, MeetingRecordDaoComponent}
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringMeetingRecordService, AttendanceMonitoringMeetingRecordServiceComponent}
import uk.ac.warwick.tabula.services.{MonitoringPointMeetingRelationshipTermService, MonitoringPointMeetingRelationshipTermServiceComponent}
import uk.ac.warwick.tabula.{Features, FeaturesComponent, Fixtures, Mockito, TestBase}

class ApproveMeetingRecordCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends ApproveMeetingRecordState with MeetingRecordDaoComponent with ApproveMeetingRecordValidation
		with MonitoringPointMeetingRelationshipTermServiceComponent with FeaturesComponent with AttendanceMonitoringMeetingRecordServiceComponent {
		val meetingRecordDao = smartMock[MeetingRecordDao]
		val monitoringPointMeetingRelationshipTermService = smartMock[MonitoringPointMeetingRelationshipTermService]
		val features = smartMock[Features]
		val attendanceMonitoringMeetingRecordService = smartMock[AttendanceMonitoringMeetingRecordService]
	}

	trait Fixture {
		val student = Fixtures.student(universityId = "0123456")
		
		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val relationship = ExternalStudentRelationship("Professor A Tutor", relationshipType, student)
		val meetingRecord = new MeetingRecord
		meetingRecord.relationship = relationship
		val proposedApproval = new MeetingRecordApproval
		proposedApproval.meetingRecord = meetingRecord

		meetingRecord.approvals.add(proposedApproval)

		val cmd = new ApproveMeetingRecordCommand(meetingRecord.approvals.get(0)) with CommandTestSupport
		cmd.features.attendanceMonitoringMeetingPointType returns true
	}

	@Test
	def testApplyTrue() {
		new Fixture {
			cmd.approved = true
			val approval = cmd.applyInternal()
			approval.meetingRecord.isApproved should be {true}
			there was one(cmd.meetingRecordDao).saveOrUpdate(approval)
			there was one(cmd.monitoringPointMeetingRelationshipTermService).updateCheckpointsForMeeting(approval.meetingRecord)
		}
	}

	@Test
	def testApplyFalse() {
		new Fixture {
			cmd.approved = false
			val approval = cmd.applyInternal()
			approval.meetingRecord.isApproved should be {false}
			there was one(cmd.meetingRecordDao).saveOrUpdate(approval)
			there was one(cmd.monitoringPointMeetingRelationshipTermService).updateCheckpointsForMeeting(approval.meetingRecord)
		}
	}

	@Test
	def validApproval() {
		val student = Fixtures.student(universityId = "0123456")
		
		val meetingRecord = new MeetingRecord

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val relationship = ExternalStudentRelationship("Professor A Tutor", relationshipType, student)
		meetingRecord.relationship = relationship
		val proposedApproval = new MeetingRecordApproval
		proposedApproval.meetingRecord = meetingRecord

		meetingRecord.approvals.add(proposedApproval)

		val cmd = new ApproveMeetingRecordCommand(meetingRecord.approvals.get(0)) with CommandTestSupport
		cmd.approved = true

		val errors = new BindException(cmd, "command")


		cmd.validate(errors)
		errors.hasErrors should be {false}
	}

	@Test
	def deletedMeetingRecord() {
		val student = Fixtures.student(universityId = "0123456")
		
		val meetingRecord = new MeetingRecord

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val relationship = ExternalStudentRelationship("Professor A Tutor", relationshipType, student)
		meetingRecord.relationship = relationship
		val proposedApproval = new MeetingRecordApproval
		proposedApproval.meetingRecord = meetingRecord

		meetingRecord.approvals.add(proposedApproval)
		meetingRecord.markDeleted()

		val cmd = new ApproveMeetingRecordCommand(meetingRecord.approvals.get(0)) with CommandTestSupport
		cmd.approved = true

		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getGlobalError.getCode should be ("meetingRecordApproval.meetingRecord.deleted")

	}

}
