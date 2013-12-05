package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSet}
import uk.ac.warwick.tabula.commands.{Unaudited, MemberOrUser, Command}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands.ReadOnly
import scala.collection.JavaConverters._

/**
 * Generates a list of students on small group, for displaying to
 * owners or tutors of that group.
 */
class ListGroupUnallocatedStudentsCommand(val smallGroupSet: SmallGroupSet) extends Command[Seq[MemberOrUser]] with Unaudited with ReadOnly {

	var profileService = Wire[ProfileService]

	PermissionCheck(Permissions.SmallGroups.ReadMembership, smallGroupSet)

	override def applyInternal() = {
		val studentsInGroups = smallGroupSet.groups.asScala.flatMap(_.students.users)


		(smallGroupSet.allStudents diff studentsInGroups) map { user =>
			val member = profileService.getMemberByUniversityId(user.getWarwickId)
			MemberOrUser(member, user)
		}
	}

}
