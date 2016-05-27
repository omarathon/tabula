package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.springframework.validation.BindException
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.{ExternalStudentRelationship, MeetingRecord, MeetingRecordApproval, StudentRelationshipType}
import uk.ac.warwick.tabula.data.{MeetingRecordDao, MeetingRecordDaoComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringMeetingRecordService, AttendanceMonitoringMeetingRecordServiceComponent}
import uk.ac.warwick.tabula.services.{MonitoringPointMeetingRelationshipTermService, MonitoringPointMeetingRelationshipTermServiceComponent, SecurityService, SecurityServiceComponent}

class ApproveMeetingRecordCommandTest extends TestBase with Mockito {

	trait CommandTestSupport extends ApproveMeetingRecordState with MeetingRecordDaoComponent with ApproveMeetingRecordValidation
		with MonitoringPointMeetingRelationshipTermServiceComponent with FeaturesComponent
		with AttendanceMonitoringMeetingRecordServiceComponent with SecurityServiceComponent {
		val meetingRecordDao = smartMock[MeetingRecordDao]
		val monitoringPointMeetingRelationshipTermService = smartMock[MonitoringPointMeetingRelationshipTermService]
		val features = smartMock[Features]
		val attendanceMonitoringMeetingRecordService = smartMock[AttendanceMonitoringMeetingRecordService]
		val securityService = smartMock[SecurityService]
	}

	trait Fixture {
		val student = Fixtures.student(universityId = "0123456")
		val studentCurrentUser = new CurrentUser(student.asSsoUser, student.asSsoUser)

		val relationshipType = StudentRelationshipType("tutor", "tutor", "personal tutor", "personal tutee")
		val relationship = ExternalStudentRelationship("Professor A Tutor", relationshipType, student)
		val meetingRecord = new MeetingRecord
		meetingRecord.relationship = relationship
		val proposedApproval = new MeetingRecordApproval
		proposedApproval.meetingRecord = meetingRecord
		proposedApproval.approver = student

		meetingRecord.approvals.add(proposedApproval)

		val cmd = new ApproveMeetingRecordCommand(meetingRecord, studentCurrentUser) with CommandTestSupport
		cmd.features.attendanceMonitoringMeetingPointType returns true
		cmd.securityService.can(studentCurrentUser, Permissions.Profiles.MeetingRecord.Approve, proposedApproval) returns true
	}

	@Test
	def testApplyTrue() {
		new Fixture {
			cmd.approved = true
			cmd.applyInternal()
			meetingRecord.isApproved should be {true}
			verify(cmd.meetingRecordDao, times(1)).saveOrUpdate(proposedApproval)
			verify(cmd.monitoringPointMeetingRelationshipTermService, times(1)).updateCheckpointsForMeeting(meetingRecord)
		}
	}

	@Test
	def testApplyFalse() {
		new Fixture {
			cmd.approved = false
			cmd.applyInternal()
			meetingRecord.isApproved should be {false}
			verify(cmd.meetingRecordDao, times(1)).saveOrUpdate(proposedApproval)
			verify(cmd.monitoringPointMeetingRelationshipTermService, times(1)).updateCheckpointsForMeeting(meetingRecord)
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
		proposedApproval.approver = student

		meetingRecord.approvals.add(proposedApproval)

		val cmd = new ApproveMeetingRecordCommand(meetingRecord, new CurrentUser(student.asSsoUser, student.asSsoUser)) with CommandTestSupport
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
		proposedApproval.approver = student

		meetingRecord.approvals.add(proposedApproval)
		meetingRecord.markDeleted()

		val cmd = new ApproveMeetingRecordCommand(meetingRecord, new CurrentUser(student.asSsoUser, student.asSsoUser)) with CommandTestSupport
		cmd.approved = true

		val errors = new BindException(cmd, "command")
		cmd.validate(errors)
		errors.hasErrors should be {true}
		errors.getErrorCount should be (1)
		errors.getGlobalError.getCode should be ("meetingRecordApproval.meetingRecord.deleted")

	}

}
