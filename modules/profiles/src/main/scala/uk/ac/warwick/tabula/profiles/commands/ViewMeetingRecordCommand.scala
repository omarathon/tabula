package uk.ac.warwick.tabula.profiles.commands
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.{AutowiringMeetingRecordDaoComponent, MeetingRecordDaoComponent, MeetingRecordDao}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.CurrentUser
import scala.Some
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import scala.Some
import scala.Some

object ViewMeetingRecordCommand{
	def apply(studentCourseDetails: StudentCourseDetails, currentUser: CurrentUser, relationshipType:RelationshipType)  =
		new ViewMeetingRecordCommandInternal(studentCourseDetails, currentUser, relationshipType) with
			AutowiringProfileServiceComponent with
			AutowiringMeetingRecordDaoComponent with
		  AutowiringRelationshipServiceComponent with
	  	ComposableCommand[Seq[MeetingRecord]] with
		  ViewMeetingRecordCommandPermissions with
	   	Unaudited

}

trait ViewMeetingRecordCommandState{
	val studentCourseDetails: StudentCourseDetails
	val requestingUser: CurrentUser
	val relationshipType:RelationshipType
}

class ViewMeetingRecordCommandInternal(val  studentCourseDetails: StudentCourseDetails, val requestingUser: CurrentUser, val relationshipType:RelationshipType)
	extends CommandInternal[Seq[MeetingRecord]] with ViewMeetingRecordCommandState {

	this:ProfileServiceComponent with RelationshipServiceComponent with  MeetingRecordDaoComponent=>


	def applyInternal() = {
    val rels = relationshipService.getRelationships(relationshipType, studentCourseDetails.sprCode)
		val currentMember = profileService.getMemberByUniversityId(requestingUser.universityId)

		currentMember match {
			case None => Seq()
			case Some(mem)=> meetingRecordDao.list(rels.toSet, mem)
		}
	}
}

trait ViewMeetingRecordCommandPermissions extends RequiresPermissionsChecking{
	this:ViewMeetingRecordCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(MeetingPermissions.Read.permissionFor(relationshipType), studentCourseDetails)

	}
}
