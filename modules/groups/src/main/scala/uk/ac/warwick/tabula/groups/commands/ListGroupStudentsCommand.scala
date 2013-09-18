package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.commands.{Unaudited, MemberOrUser, Command}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.permissions.Permissions

/**
 * Generates a list of students on small group, for displaying to
 * owners or tutors of that group.
 */
class ListGroupStudentsCommand(val group: SmallGroup) extends Command[Seq[MemberOrUser]] with Unaudited {

	var profileService = Wire[ProfileService]

	PermissionCheck(Permissions.SmallGroups.ReadMembership, group)

	override def applyInternal() = {
		group.students.users map { user =>
			val member = profileService.getMemberByUniversityId(user.getWarwickId)
			MemberOrUser(member, user)
		}
	}

}
