package uk.ac.warwick.tabula.commands.profiles.relationships.meetings

import org.joda.time.DateTime
import uk.ac.warwick.tabula.DateFormats.DateTimePickerFormatter
import org.springframework.validation.ValidationUtils._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.FeaturesComponent
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringMeetingRecordServiceComponent
import uk.ac.warwick.tabula.services.{FileAttachmentServiceComponent, MeetingRecordServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

abstract class AbstractModifyMeetingRecordCommand extends AbstractMeetingRecordCommand with CommandInternal[MeetingRecord] {
	self: ModifyMeetingRecordCommandState with MeetingRecordCommandRequest with MeetingRecordServiceComponent
		with FeaturesComponent with AttendanceMonitoringMeetingRecordServiceComponent
		with FileAttachmentServiceComponent =>
}



trait ModifyMeetingRecordValidation extends MeetingRecordValidation {

	self: MeetingRecordCommandRequest with ModifyMeetingRecordCommandState =>

	override def validate(errors: Errors) {
		super.validate(errors)
		rejectIfEmptyOrWhitespace(errors, "relationship", "NotEmpty")

		if(DateTimePickerFormatter.parseDateTime(meetingDateStr+" "+meetingTimeStr).compareTo(DateTime.now) > 0){
			errors.rejectValue("meetingDateStr", "meetingRecord.date.future")
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

trait ModifyMeetingRecordCommandState extends MeetingRecordCommandState {
	def relationship: StudentRelationship
}


