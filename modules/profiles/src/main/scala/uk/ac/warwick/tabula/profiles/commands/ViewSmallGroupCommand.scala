package uk.ac.warwick.tabula.profiles.commands

import scala.collection.JavaConverters._

import uk.ac.warwick.tabula.commands.{Description, Command}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.MemberDao
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService

/**
 *
 */
class ViewSmallGroupCommand(val user: CurrentUser, val smallGroup: SmallGroup) extends Command[Seq[Member]] {

	val memberDao = Wire[MemberDao]
	val userLookup = Wire[UserLookupService]

	PermissionCheck(Permissions.SmallGroups.Read, smallGroup)

	def applyInternal() = {
		smallGroup.students.users.flatMap(u => memberDao.getByUniversityId(u.getWarwickId))
	}

	def describe(d: Description) {
		d.smallGroup(smallGroup)
	}
}
