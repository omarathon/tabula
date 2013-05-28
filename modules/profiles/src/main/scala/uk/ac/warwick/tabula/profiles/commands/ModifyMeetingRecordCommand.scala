package uk.ac.warwick.tabula.profiles.commands

import org.joda.time.DateTime
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils.rejectIfEmptyOrWhitespace
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.MeetingRecordDao
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.BindListener
import collection.JavaConverters._
import uk.ac.warwick.tabula.data.FileDao
import org.joda.time.LocalDate
import scala.Some
import uk.ac.warwick.tabula.data.model.MeetingApprovalState.Pending
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.JavaImports.JArrayList


abstract class ModifyMeetingRecordCommand(val creator: Member, var relationship: StudentRelationship)
	extends Command[MeetingRecord] with SelfValidating with FormattedHtml with BindListener with Daoisms {

	var features = Wire.auto[Features]
	var meetingRecordDao = Wire.auto[MeetingRecordDao]
	var fileDao = Wire.auto[FileDao]

	val HOUR = 12 // arbitrary meeting time
	val PREHISTORIC_YEARS = 5 // number of years to consider as extremely old

	var title: String = _
	var description: String = _
	var meetingDate: LocalDate = _
	var format: MeetingFormat = _

	var file: UploadedFile = new UploadedFile
	var attachedFiles:JList[FileAttachment] = _

	var attachmentTypes = Seq[String]()

	var posted: Boolean = false

	PermissionCheck(Permissions.Profiles.MeetingRecord.Create, relationship.studentMember)

	// Gets the meeting record to be updated.
	def getMeetingRecord: MeetingRecord

	override def applyInternal() = {

		def persistAttachments(meeting: MeetingRecord) {
			// delete attachments that have been removed

			if (meeting.attachments != null){
				val filesToKeep = Option(attachedFiles).map(_.asScala.toList).getOrElse(List())
				val filesToRemove = (meeting.attachments.asScala -- filesToKeep)
				meeting.attachments = JArrayList[FileAttachment](filesToKeep)
				filesToRemove.foreach(session.delete(_))
			}

			file.attached.asScala map(attachment => {
				attachment.meetingRecord = meeting
				meeting.attachments.add(attachment)
				fileDao.savePermanent(attachment)
			})
		}

		val meeting = getMeetingRecord
		meeting.title = title
		meeting.description = description
		meeting.meetingDate = meetingDate.toDateTimeAtStartOfDay().withHourOfDay(HOUR) // arbitrarily record as noon
		meeting.format = format
		persistAttachments(meeting)

		// persist the meeting record
		meetingRecordDao.saveOrUpdate(meeting)

		//if (features.meetingRecordApproval){
			val meetingApprovals = updateMeetingApproval(meeting)
			meetingApprovals.foreach(meetingApproval => {
				//TODO-Ritchie notification
			})
		//}

		meeting
	}

	def updateMeetingApproval(meetingRecord: MeetingRecord) : Option[MeetingRecordApproval] = {

		def getMeetingRecord(approver: Member) : MeetingRecordApproval = {

			val meetingRecordApproval = meetingRecord.approvals.asScala.find(_.approver == approver).getOrElse{
				val newMeetingRecordApproval = new MeetingRecordApproval()
				newMeetingRecordApproval.approver = approver
				newMeetingRecordApproval.meetingRecord = meetingRecord
				meetingRecord.approvals.add(newMeetingRecordApproval)
				newMeetingRecordApproval
			}

			meetingRecordApproval.state = Pending
			session.saveOrUpdate(meetingRecordApproval)
			meetingRecordApproval
		}

		val approver = Seq(relationship.agentMember, Some(relationship.studentMember)).flatten.find(_ != creator)
		approver.map(getMeetingRecord(_))

	}

	override def validate(errors: Errors) {
		rejectIfEmptyOrWhitespace(errors, "title", "NotEmpty")
		if (title.length > 500){errors.rejectValue("title", "meetingRecord.title.long")}

		rejectIfEmptyOrWhitespace(errors, "format", "NotEmpty")

		meetingDate match {
			case date:LocalDate => {
				if (meetingDate.isAfter(DateTime.now.toLocalDate)) {
					errors.rejectValue("meetingDate", "meetingRecord.date.future")
				} else if (meetingDate.isBefore(DateTime.now.minusYears(PREHISTORIC_YEARS).toLocalDate)) {
					errors.rejectValue("meetingDate", "meetingRecord.date.prehistoric")
				}
			}
			case _ => errors.rejectValue("meetingDate", "meetingRecord.date.missing")
		}
	}

}