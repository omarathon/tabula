package uk.ac.warwick.tabula.profiles.commands
import org.springframework.validation.ValidationUtils._

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Command,Unaudited}
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.MeetingRecordDao
import uk.ac.warwick.tabula.data.model.MeetingRecord
import uk.ac.warwick.tabula.data.model.RelationshipType._
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.RelationshipService


class ViewMeetingRecordCommand(val studentCourseDetails: StudentCourseDetails, val currentUser: CurrentUser) extends Command[Seq[MeetingRecord]] with Unaudited {

	PermissionCheck(Permissions.Profiles.MeetingRecord.Read, studentCourseDetails.student)

	var dao = Wire.auto[MeetingRecordDao]
	var relationshipService = Wire.auto[RelationshipService]
	var profileService = Wire.auto[ProfileService]

	def applyInternal() = {
		val rels = relationshipService.getRelationships(PersonalTutor, studentCourseDetails.sprCode)
		val currentMember = profileService.getMemberByUniversityId(currentUser.universityId)

		currentMember match {
			case None => Seq()
			case Some(mem)=> dao.list(rels.toSet, mem)
		}
	}
}
