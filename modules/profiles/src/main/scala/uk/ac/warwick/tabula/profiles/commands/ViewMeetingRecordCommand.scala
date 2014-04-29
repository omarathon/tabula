package uk.ac.warwick.tabula.profiles.commands
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions

object ViewMeetingRecordCommand{
	def apply(studentCourseDetails: StudentCourseDetails, currentMember: Option[Member], relationshipType: StudentRelationshipType)  =
		new ViewMeetingRecordCommandInternal(studentCourseDetails, currentMember, relationshipType) with
			ComposableCommand[Seq[AbstractMeetingRecord]] with
			AutowiringProfileServiceComponent with
			AutowiringMeetingRecordServiceComponent with
			AutowiringRelationshipServiceComponent with
			ViewMeetingRecordCommandPermissions with
			ReadOnly with Unaudited
}

trait ViewMeetingRecordCommandState{
	val studentCourseDetails: StudentCourseDetails
	val currentMember: Option[Member]
	val relationshipType: StudentRelationshipType
}

class ViewMeetingRecordCommandInternal(
	val studentCourseDetails: StudentCourseDetails,
	val currentMember: Option[Member],
	val relationshipType: StudentRelationshipType
) extends CommandInternal[Seq[AbstractMeetingRecord]] with ViewMeetingRecordCommandState {

	this: ProfileServiceComponent with RelationshipServiceComponent with MeetingRecordServiceComponent =>

	def applyInternal() = {
		val rels = relationshipService.getRelationships(relationshipType, studentCourseDetails.student)
		meetingRecordService.listAll(rels.toSet, currentMember)
	}
}

trait ViewMeetingRecordCommandPermissions extends RequiresPermissionsChecking {
	this:ViewMeetingRecordCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.MeetingRecord.Read(relationshipType), studentCourseDetails)
	}
}