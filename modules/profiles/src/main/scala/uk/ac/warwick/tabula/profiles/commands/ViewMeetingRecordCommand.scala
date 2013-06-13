package uk.ac.warwick.tabula.profiles.commands
import scala.reflect.BeanProperty
import org.joda.time.DateTime
import org.springframework.validation.Errors
import org.springframework.validation.ValidationUtils._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Command,Unaudited}
import uk.ac.warwick.tabula.data.MeetingRecordDao
import uk.ac.warwick.tabula.data.model.{Member,StudentMember,StudentRelationship,MeetingRecord}
import uk.ac.warwick.tabula.data.model.RelationshipType._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.MeetingRecordDao
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.StudentCourseDetails


class ViewMeetingRecordCommand(val studentCourseDetails: StudentCourseDetails, val currentUser: CurrentUser) extends Command[Seq[MeetingRecord]] with Unaudited {

	PermissionCheck(Permissions.Profiles.MeetingRecord.Read, studentCourseDetails.student)

	var dao = Wire.auto[MeetingRecordDao]
	var profileService = Wire.auto[ProfileService]

	def applyInternal() = {
		val rels = profileService.getRelationships(PersonalTutor, studentCourseDetails.sprCode)
		val currentMember = profileService.getMemberByUniversityId(currentUser.universityId)

		currentMember match {
			case None => Seq()
			case Some(mem)=> dao.list(rels.toSet, mem)
		}
	}
}
