package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.data.model.MeetingApprovalState._
import org.springframework.validation.BindException
import uk.ac.warwick.tabula.PersistenceTestBase
import uk.ac.warwick.tabula.data.{MeetingRecordDao, MeetingRecordDaoComponent}
import uk.ac.warwick.tabula.services.{MonitoringPointMeetingRelationshipTermService, MonitoringPointMeetingRelationshipTermServiceComponent}

class EditMeetingRecordCommandTest extends PersistenceTestBase with MeetingRecordTests {

	@Test
	def creatorEditMeeting = withUser("cuscav") {  withFakeTime(aprilFool) {

		val cmd = new EditMeetingRecordCommand(meeting){
			override val session = mockSession
		}
		cmd.copyToCommand(meeting)
		cmd.title = "Updated title fools"
		cmd.maintenanceMode = maintenanceModeService
		cmd.notificationService = notificationService
		cmd.meetingRecordDao = meetingRecordDao
		cmd.features = emptyFeatures
		cmd.features.meetingRecordApproval = true
		meeting = transactional { tx => cmd.apply() }
		meeting.title should be ("Updated title fools")

		meeting.isPendingApproval should be (true)
		meeting.pendingApprovalBy(student) should be (true)
	}}

	@Test
	def meetingRecordWorkflow = withUser("cuslaj") {  withFakeTime(aprilFool) {

		// Here is a story about the meeting record workflow ...
		// A student sees a meeting record with an inaccurate description. She tries to reject but forgets to add a comment

		var approvalCmd = new ApproveMeetingRecordCommand(meeting.approvals.get(0)) with ApproveMeetingRecordState with MeetingRecordDaoComponent
			with ApproveMeetingRecordValidation with MonitoringPointMeetingRelationshipTermServiceComponent {
				val meetingRecordDao = mock[MeetingRecordDao]
				val monitoringPointMeetingRelationshipTermService = mock[MonitoringPointMeetingRelationshipTermService]
			}

		approvalCmd.approved = false
		val errors = new BindException(approvalCmd, "command")
		approvalCmd.validate(errors)
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("rejectionComments")
		errors.getFieldError.getCode should be ("meetingRecordApproval.rejectionComments.isEmpty")

		// Validation prompts them for a rejection comment. They rant about herons and reject.
		val heronRant = "There is no mention of herons in the meeting record. I distinctly remember LOADS of herons in my face."
		approvalCmd.rejectionComments = heronRant
		var approval = transactional { tx => approvalCmd.applyInternal() }
		approval.state should be (Rejected)
		approval.comments should be (heronRant)
		meeting.isRejected should be (true)
		meeting.pendingRevisionBy(creator) should be (true)

		// The tutor sees the rejection. They add a description about herons to placate the student.
		val editCmd = new EditMeetingRecordCommand(meeting) {
			override val session = mockSession
		}
		editCmd.features = emptyFeatures
		editCmd.features.meetingRecordApproval = true

		editCmd.meetingRecordDao = meetingRecordDao
		editCmd.notificationService = notificationService
		editCmd.copyToCommand(meeting)
		editCmd.description = "The meeting room was full of angry herons. It was truly harrowing."
		val meeting2 = transactional { tx => editCmd.apply() }
		meeting2.isPendingApproval should be (true)
		meeting2.pendingApprovalBy(student) should be (true)

		// The student is now happy with the record and approves it
		approvalCmd = new ApproveMeetingRecordCommand(meeting.approvals.get(0)) with ApproveMeetingRecordState with MeetingRecordDaoComponent
			with ApproveMeetingRecordValidation with MonitoringPointMeetingRelationshipTermServiceComponent {
				val meetingRecordDao = mock[MeetingRecordDao]
				val monitoringPointMeetingRelationshipTermService = mock[MonitoringPointMeetingRelationshipTermService]
			}
		approvalCmd.approved = true
		approvalCmd.rejectionComments = null
		approval = transactional { tx => approvalCmd.applyInternal() }
		meeting2.isApproved should be (true)
		// Fin
	}}

}
