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
import uk.ac.warwick.tabula.permissions.Permissions

object ViewMeetingRecordCommand{
	def apply(studentCourseDetails: StudentCourseDetails, currentUser: CurrentUser, relationshipType: StudentRelationshipType)  =
		new ViewMeetingRecordCommandInternal(studentCourseDetails, currentUser, relationshipType) with
			AutowiringProfileServiceComponent with
			AutowiringMeetingRecordDaoComponent with
			AutowiringRelationshipServiceComponent
}

trait ViewMeetingRecordCommandState{
	val studentCourseDetails: StudentCourseDetails
	val requestingUser: CurrentUser
	val relationshipType: StudentRelationshipType
}

class ViewMeetingRecordCommandInternal(val  studentCourseDetails: StudentCourseDetails, val requestingUser: CurrentUser, val relationshipType: StudentRelationshipType)
	extends Command[Seq[MeetingRecord]] with ViewMeetingRecordCommandState with Unaudited {

	this: ProfileServiceComponent with RelationshipServiceComponent with MeetingRecordDaoComponent =>

	PermissionCheck(Permissions.Profiles.MeetingRecord.Read(relationshipType), studentCourseDetails)

	def applyInternal() = {
		val rels = relationshipService.getRelationships(relationshipType, studentCourseDetails.sprCode)
		val currentMember = profileService.getMemberByUniversityId(requestingUser.universityId)

		currentMember match {
			case None => Seq()
			case Some(mem)=> meetingRecordDao.list(rels.toSet, mem)
		}
	}
}