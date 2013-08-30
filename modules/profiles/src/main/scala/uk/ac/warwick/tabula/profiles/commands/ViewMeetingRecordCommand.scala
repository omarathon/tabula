package uk.ac.warwick.tabula.profiles.commands
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.{AutowiringMeetingRecordDaoComponent, MeetingRecordDaoComponent, MeetingRecordDao}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions

object ViewMeetingRecordCommand{
	def apply(studentCourseDetails: StudentCourseDetails, currentUser: CurrentUser, relationshipType: StudentRelationshipType)  =
		new ViewMeetingRecordCommandInternal(studentCourseDetails, currentUser, relationshipType) with
			ComposableCommand[Seq[MeetingRecord]] with
			AutowiringProfileServiceComponent with
			AutowiringMeetingRecordDaoComponent with
			AutowiringRelationshipServiceComponent with
			ViewMeetingRecordCommandPermissions with
			Unaudited
}

trait ViewMeetingRecordCommandState{
	val studentCourseDetails: StudentCourseDetails
	val requestingUser: CurrentUser
	val relationshipType: StudentRelationshipType
}

class ViewMeetingRecordCommandInternal(val  studentCourseDetails: StudentCourseDetails, val requestingUser: CurrentUser, val relationshipType: StudentRelationshipType)
	extends CommandInternal[Seq[MeetingRecord]] with ViewMeetingRecordCommandState {

	this: ProfileServiceComponent with RelationshipServiceComponent with MeetingRecordDaoComponent =>

	def applyInternal() = {
		val rels = relationshipService.getRelationships(relationshipType, studentCourseDetails.sprCode)
		val currentMember = profileService.getMemberByUniversityId(requestingUser.universityId)

		currentMember match {
			case None => Seq()
			case Some(mem)=> meetingRecordDao.list(rels.toSet, mem)
		}
	}
}

trait ViewMeetingRecordCommandPermissions extends RequiresPermissionsChecking {
	this:ViewMeetingRecordCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.MeetingRecord.Read(relationshipType), studentCourseDetails)
	}
}