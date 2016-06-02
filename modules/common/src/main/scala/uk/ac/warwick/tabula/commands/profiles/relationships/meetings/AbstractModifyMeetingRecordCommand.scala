package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.{DateTime, LocalDate}
import org.springframework.validation.ValidationUtils._
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.FeaturesComponent
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.MeetingApprovalState.Pending
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.services.{FileAttachmentServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

abstract class AbstractModifyMeetingRecordCommand extends CommandInternal[MeetingRecord] {

	self: ModifyMeetingRecordCommandRequest with MeetingRecordServiceComponent
	with FeaturesComponent with AttendanceMonitoringMeetingRecordServiceComponent
	with FileAttachmentServiceComponent =>

	protected def applyCommon(meeting: MeetingRecord): MeetingRecord = {
		meeting.title = title
		meeting.description = description
		meeting.isRealTime match {
			case true => meeting.meetingDate = meetingDateTime
			case false => meeting.meetingDate = meetingDate.toDateTimeAtStartOfDay.withHourOfDay(MeetingRecord.DefaultMeetingTimeOfDay)
		}
		meeting.format = format
		meeting.lastUpdatedDate = DateTime.now
		persistAttachments(meeting)

		// persist the meeting record
		meetingRecordService.saveOrUpdate(meeting)

		if (features.meetingRecordApproval) {
			updateMeetingApproval(meeting)
		}

		if (features.attendanceMonitoringMeetingPointType) {
			attendanceMonitoringMeetingRecordService.updateCheckpoints(meeting)
		}

		meeting
	}

	private def persistAttachments(meeting: MeetingRecord) {
		// delete attachments that have been removed

		if (meeting.attachments != null) {
			val filesToKeep = Option(attachedFiles).map(_.asScala.toList).getOrElse(List())
			val filesToRemove = meeting.attachments.asScala -- filesToKeep
			meeting.attachments = JArrayList[FileAttachment](filesToKeep)
			fileAttachmentService.deleteAttachments(filesToRemove)
		}

		file.attached.asScala.foreach(attachment => {
			attachment.meetingRecord = meeting
			meeting.attachments.add(attachment)
			fileAttachmentService.savePermanant(attachment)
		})
	}

	private def updateMeetingApproval(meetingRecord: MeetingRecord) : Option[MeetingRecordApproval] = {

		def getMeetingRecord(approver: Member) : MeetingRecordApproval = {

			val meetingRecordApproval = meetingRecord.approvals.asScala.find(_.approver == approver).getOrElse{
				val newMeetingRecordApproval = new MeetingRecordApproval()
				newMeetingRecordApproval.approver = approver
				newMeetingRecordApproval.meetingRecord = meetingRecord
				meetingRecord.approvals.add(newMeetingRecordApproval)
				newMeetingRecordApproval
			}

			meetingRecordApproval.state = Pending
			meetingRecordService.saveOrUpdate(meetingRecordApproval)
			meetingRecordApproval
		}

		val approver = Seq(meetingRecord.relationship.agentMember, meetingRecord.relationship.studentMember).flatten.find(_ != meetingRecord.creator)
		approver.map(getMeetingRecord)

	}

}

trait ModifyMeetingRecordCommandBindListener extends BindListener {

	self: ModifyMeetingRecordCommandRequest =>

	override def onBind(result:BindingResult) = transactional() {
		file.onBind(result)
	}

}

trait ModifyMeetingRecordValidation extends SelfValidating {

	self: ModifyMeetingRecordCommandRequest with ModifyMeetingRecordCommandState =>

	override def validate(errors: Errors) {
		rejectIfEmptyOrWhitespace(errors, "title", "NotEmpty")
		if (title.length > MeetingRecord.MaxTitleLength){
			errors.rejectValue("title", "meetingRecord.title.long", new Array(MeetingRecord.MaxTitleLength), "")
		}

		rejectIfEmptyOrWhitespace(errors, "relationship", "NotEmpty")
		rejectIfEmptyOrWhitespace(errors, "format", "NotEmpty")

		val dateToCheck: DateTime = isRealTime match {
			case true => meetingDateTime
			case false => meetingDate.toDateTimeAtStartOfDay
		}

		if (dateToCheck == null) {
			errors.rejectValue("meetingDate", "meetingRecord.date.missing")
			errors.rejectValue("meetingDateTime", "meetingRecord.date.missing")
		} else {
			if (dateToCheck.isAfter(DateTime.now)) {
				errors.rejectValue("meetingDate", "meetingRecord.date.future")
				errors.rejectValue("meetingDateTime", "meetingRecord.date.future")
			} else if (dateToCheck.isBefore(DateTime.now.minusYears(MeetingRecord.MeetingTooOldThresholdYears))) {
				errors.rejectValue("meetingDate", "meetingRecord.date.prehistoric")
				errors.rejectValue("meetingDateTime", "meetingRecord.date.prehistoric")
			}
		}
	}

}

trait ModifyMeetingRecordPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ModifyMeetingRecordCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.MeetingRecord.Manage(relationship.relationshipType), mandatory(relationship.studentMember))
	}

}

trait ModifyMeetingRecordDescription extends Describable[MeetingRecord] {

	self: ModifyMeetingRecordCommandState =>

	override def describe(d: Description) {
		relationship.studentMember.map(d.member)
		d.properties(
			"creator" -> creator.universityId,
			"relationship" -> relationship.relationshipType.toString()
		)
	}

	override def describeResult(d: Description, meeting: MeetingRecord) {
		relationship.studentMember.map(d.member)
		d.properties(
			"creator" -> creator.universityId,
			"relationship" -> relationship.relationshipType.toString(),
			"meeting" -> meeting.id
		)
		d.fileAttachments(meeting.attachments.asScala)
	}
}

trait ModifyMeetingRecordCommandState {
	def creator: Member
	def relationship: StudentRelationship
	val attachmentTypes = Seq[String]()
	var isRealTime: Boolean = true
}

trait ModifyMeetingRecordCommandRequest {
	var title: String = _
	var description: String = _
	var meetingDateTime: DateTime = DateTime.now.hourOfDay.roundFloorCopy
	var meetingDate: LocalDate = _
	var format: MeetingFormat = _
	var file: UploadedFile = new UploadedFile
	var attachedFiles: JList[FileAttachment] = _
}
