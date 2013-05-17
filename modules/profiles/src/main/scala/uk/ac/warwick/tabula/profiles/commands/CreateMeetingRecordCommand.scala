package uk.ac.warwick.tabula.profiles.commands

import scala.reflect.BeanProperty

import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.springframework.validation.BindingResult
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils.rejectIfEmptyOrWhitespace
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.commands.UploadedFile
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.MeetingRecordDao
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.BindListener
import collection.JavaConversions._
import uk.ac.warwick.tabula.data.FileDao
import org.springframework.web.multipart.MultipartFile
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import scala.Some
import uk.ac.warwick.tabula.data.model.MeetingApprovalState.Pending
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.Features

class CreateMeetingRecordCommand(
		val creator: Member,
		var relationship: StudentRelationship)
	extends Command[MeetingRecord] with SelfValidating with FormattedHtml with BindListener with Daoisms {

	@Autowired var features: Features = _

	val HOUR = 12 // arbitrary meeting time
	val PREHISTORIC_YEARS = 5 // number of years to consider as extremely old

	var title: String = _
	var description: String = _
	var meetingDate: LocalDate = DateTime.now.toLocalDate
	var format: MeetingFormat = _

	var file: UploadedFile = new UploadedFile

	var attachmentTypes = Seq[String]()

	PermissionCheck(Permissions.Profiles.MeetingRecord.Create, relationship.studentMember)

	var meetingRecordDao = Wire.auto[MeetingRecordDao]
	var fileDao = Wire.auto[FileDao]

	def applyInternal() = {

		def doFiling(meeting: MeetingRecord) {
			file.attached map(attachment => {
				attachment.meetingRecord = meeting
				meeting.attachments.add(attachment)
				fileDao.savePermanent(attachment)
			})
		}

		val meeting = new MeetingRecord(creator, relationship)
		meeting.title = title
		meeting.description = formattedHtml(description)
		meeting.meetingDate = meetingDate.toDateTimeAtStartOfDay().withHourOfDay(HOUR) // arbitrarily record as noon

		meeting.format = format
		doFiling(meeting)

		// persist the meeting record
		meetingRecordDao.saveOrUpdate(meeting)

		if (features.meetingRecordApproval){
			val meetingApprovals = generateMeetingApproval(meeting)
			meetingApprovals.foreach(meetingApproval => {
				meeting.approvals.add(meetingApproval)
				//TODO-Ritchie notification
			})
		}

		meeting
	}

	def generateMeetingApproval(meetingRecord: MeetingRecord) : Option[MeetingRecordApproval] = {

		def newMeetingRecord(approver: Member) : MeetingRecordApproval = {
			val meetingRecordApproval = new MeetingRecordApproval()
			meetingRecordApproval.state = Pending
			meetingRecordApproval.approver = approver
			meetingRecordApproval.meetingRecord = meetingRecord
			session.saveOrUpdate(meetingRecordApproval)
			meetingRecordApproval
		}

		val approver = Seq(relationship.agentMember, Some(relationship.studentMember)).flatten.find(_ != creator)
		approver.map(newMeetingRecord(_))

	}

	override def onBind(result:BindingResult) = transactional() {
		file.onBind(result)
	}

	def validate(errors: Errors) {
		rejectIfEmptyOrWhitespace(errors, "title", "NotEmpty")
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

	def describe(d: Description): Unit = {  }

}
