package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.{LocalDate, DateTime}
import org.springframework.validation.ValidationUtils._
import org.springframework.validation.{Errors, BindingResult}
import uk.ac.warwick.tabula.FeaturesComponent
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.MeetingApprovalState.Pending
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.services.{FileAttachmentServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.BindListener

import scala.collection.JavaConverters._

abstract class AbstractMeetingRecordCommand  {

	self: MeetingRecordCommandRequest with MeetingRecordServiceComponent
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

		val newAttachments = file.attached.asScala.map ( _.duplicate() )
		newAttachments.foreach(meeting.addAttachment)
	}

	private def updateMeetingApproval(meetingRecord: MeetingRecord) : Option[MeetingRecordApproval] = {

		def getMeetingRecord(approver: Member) : MeetingRecordApproval = {

			val meetingRecordApproval = meetingRecord.approvals.asScala.find(_.approver == approver).getOrElse {
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

trait MeetingRecordCommandBindListener extends BindListener {

	self: MeetingRecordCommandRequest =>

	override def onBind(result:BindingResult) = transactional() {
		file.onBind(result)
	}
}

trait MeetingRecordValidation extends SelfValidating {

	self: MeetingRecordCommandRequest with MeetingRecordCommandState =>

	override def validate(errors: Errors) {
		rejectIfEmptyOrWhitespace(errors, "title", "NotEmpty")
		if (title.length > MeetingRecord.MaxTitleLength){
			errors.rejectValue("title", "meetingRecord.title.long", new Array(MeetingRecord.MaxTitleLength), "")
		}

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

trait MeetingRecordCommandState {
	def creator: Member
	val attachmentTypes = Seq[String]()
	var isRealTime: Boolean = true
}

trait MeetingRecordCommandRequest {
	var title: String = _
	var description: String = _
	var meetingDateTime: DateTime = DateTime.now.hourOfDay.roundFloorCopy
	var meetingDate: LocalDate = _
	var format: MeetingFormat = _
	var file: UploadedFile = new UploadedFile
	var attachedFiles: JList[FileAttachment] = _
}
