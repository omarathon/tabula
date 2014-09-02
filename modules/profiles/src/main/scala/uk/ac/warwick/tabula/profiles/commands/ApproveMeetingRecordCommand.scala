package uk.ac.warwick.tabula.profiles.commands

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.FeaturesComponent
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.MeetingApprovalState._
import uk.ac.warwick.tabula.data.model.notifications.meetingrecord.{MeetingRecordApprovedNotification, MeetingRecordRejectedNotification}
import uk.ac.warwick.tabula.data.model.{MeetingRecord, MeetingRecordApproval, Notification}
import uk.ac.warwick.tabula.data.{AutowiringMeetingRecordDaoComponent, MeetingRecordDaoComponent}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AttendanceMonitoringMeetingRecordServiceComponent, AutowiringAttendanceMonitoringMeetingRecordServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointMeetingRelationshipTermServiceComponent, MonitoringPointMeetingRelationshipTermServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ApproveMeetingRecordCommand {
	def apply(approval: MeetingRecordApproval) =
		new ApproveMeetingRecordCommand(approval)
		with ComposableCommand[MeetingRecordApproval]
		with ApproveMeetingRecordDescription
		with ApproveMeetingRecordPermission
		with ApproveMeetingRecordValidation
		with ApproveMeetingRecordNotification
		with AutowiringMeetingRecordDaoComponent
		with AutowiringMonitoringPointMeetingRelationshipTermServiceComponent
		with AutowiringAttendanceMonitoringMeetingRecordServiceComponent

}

class ApproveMeetingRecordCommand (val approval: MeetingRecordApproval) extends CommandInternal[MeetingRecordApproval] with ApproveMeetingRecordState {

	self: MeetingRecordDaoComponent with MonitoringPointMeetingRelationshipTermServiceComponent
		with FeaturesComponent with AttendanceMonitoringMeetingRecordServiceComponent =>

	def applyInternal() = transactional() {
		if (approved) {
			approval.state = Approved
		} else {
			approval.state = Rejected
			approval.comments = rejectionComments
		}

		approval.lastUpdatedDate = DateTime.now

		meetingRecordDao.saveOrUpdate(approval)

		if (features.attendanceMonitoringMeetingPointType) {
			monitoringPointMeetingRelationshipTermService.updateCheckpointsForMeeting(approval.meetingRecord)
			if (features.attendanceMonitoringAcademicYear2014)
				attendanceMonitoringMeetingRecordService.updateCheckpoints(approval.meetingRecord)
		}

		approval
	}
}

trait ApproveMeetingRecordValidation extends SelfValidating {
	self: ApproveMeetingRecordState =>

	def validate(errors: Errors) {
		if (approval.meetingRecord.deleted){
			errors.reject("meetingRecordApproval.meetingRecord.deleted")
		}
		if (approved == null) {
			errors.rejectValue("approved", "meetingRecordApproval.approved.isNull")
		} else if (!approved && !rejectionComments.hasText) {
			errors.rejectValue("rejectionComments", "meetingRecordApproval.rejectionComments.isEmpty")
		}
	}
}

trait ApproveMeetingRecordPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ApproveMeetingRecordState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.MeetingRecord.Update(approval.meetingRecord.relationship.relationshipType), approval.meetingRecord)
	}
}

trait ApproveMeetingRecordDescription extends Describable[MeetingRecordApproval] {
	self: ApproveMeetingRecordState =>
	def describe(d: Description) {
		d.properties("meetingRecord" -> approval.meetingRecord.id)
	}
}

trait ApproveMeetingRecordNotification extends Notifies[MeetingRecordApproval, MeetingRecord] {
	self: ApproveMeetingRecordState =>

	def emit(approval: MeetingRecordApproval) = {
		val agent = approval.approver.asSsoUser

		if (approved) Seq( Notification.init(new MeetingRecordApprovedNotification, agent, Seq(approval) ))
		else Seq( Notification.init(new MeetingRecordRejectedNotification, agent, Seq(approval) ))
	}

}

trait ApproveMeetingRecordState {
	def approval: MeetingRecordApproval
	var approved: JBoolean = _
	var rejectionComments: String =_
}
