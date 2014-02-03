package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.data.model.{FileAttachment, MeetingFormat, MeetingRecord, ScheduledMeetingRecord, StudentRelationship, Member}
import uk.ac.warwick.tabula.commands.{SelfValidating, UploadedFile, Description, Describable, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.{BindingResult, Errors}
import org.springframework.validation.ValidationUtils._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.{MeetingRecordDaoComponent, AutowiringMeetingRecordDaoComponent}
import uk.ac.warwick.tabula.system.BindListener

object CreateScheduledMeetingRecordCommand {
	def apply(creator: Member, relationship: StudentRelationship, considerAlternatives: Boolean) =
		new CreateScheduledMeetingRecordCommand(creator, relationship, considerAlternatives) with ComposableCommand[ScheduledMeetingRecord] with CreateScheduledMeetingPermissions
			with CreateScheduledMeetingState with CreateScheduledMeetingRecordDescription with AutowiringMeetingRecordDaoComponent with CreateScheduledMeetingRecordCommandValidation
}

class CreateScheduledMeetingRecordCommand (val creator: Member, val relationship: StudentRelationship, val considerAlternatives: Boolean = false)
	extends CommandInternal[ScheduledMeetingRecord] with CreateScheduledMeetingState with BindListener {

	self: MeetingRecordDaoComponent =>

	def applyInternal() = {
		val scheduledMeeting = new ScheduledMeetingRecord(creator, relationship)
		scheduledMeeting.title = title
		scheduledMeeting.description = description
		scheduledMeeting.meetingDate = meetingDate.toDateTime
		scheduledMeeting.lastUpdatedDate = DateTime.now
		scheduledMeeting.creationDate = DateTime.now
		scheduledMeeting.format = format

		file.attached.asScala map(attachment => {
			attachment.meetingRecord = scheduledMeeting
				scheduledMeeting.attachments.add(attachment)
				attachment.temporary = false
		})
		meetingRecordDao.saveOrUpdate(scheduledMeeting)
		scheduledMeeting
	}

	def onBind(result: BindingResult) {
		file.onBind(result)
	}

}

trait CreateScheduledMeetingRecordCommandValidation extends SelfValidating {
	self: CreateScheduledMeetingState =>
	override def validate(errors: Errors) {
		rejectIfEmptyOrWhitespace(errors, "title", "NotEmpty")
		if (title.length > MeetingRecord.MaxTitleLength){
			errors.rejectValue("title", "meetingRecord.title.long", new Array(MeetingRecord.MaxTitleLength), "")
		}

		rejectIfEmptyOrWhitespace(errors, "relationship", "NotEmpty")
		rejectIfEmptyOrWhitespace(errors, "format", "NotEmpty")

		meetingDate match {
			case date:DateTime => {
				if (meetingDate.isBefore(DateTime.now.toDateTime)) {
					errors.rejectValue("meetingDate", "meetingRecord.date.past")
				} else if (meetingDate.isAfter(DateTime.now.plusYears(MeetingRecord.MeetingTooOldThresholdYears).toDateTime)) {
					errors.rejectValue("meetingDate", "meetingRecord.date.futuristic")
				}
			}
			case _ => errors.rejectValue("meetingDate", "meetingRecord.date.missing")
		}
	}
}

trait CreateScheduledMeetingState {
	def creator: Member
	def relationship: StudentRelationship
	def considerAlternatives: Boolean

	var title: String = _
	var description: String = _
	var meetingDate: DateTime = _
	var format: MeetingFormat = _

	var file: UploadedFile = new UploadedFile
	var attachedFiles:JList[FileAttachment] = _

	var attachmentTypes = Seq[String]()
}

trait CreateScheduledMeetingPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: CreateScheduledMeetingState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.MeetingRecord.Create(relationship.relationshipType), mandatory(relationship.studentMember))
	}
}

trait CreateScheduledMeetingRecordDescription extends Describable[ScheduledMeetingRecord] {
	self: CreateScheduledMeetingState =>

	override lazy val eventName = "CreateScheduledMeetingRecord"

	override def describe(d: Description) {
		relationship.studentMember.map { d.member(_) }
		d.properties(
			"creator" -> creator.universityId,
			"relationship" -> relationship.relationshipType.toString()
		)
	}
}
