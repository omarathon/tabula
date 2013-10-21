package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent, UserLookupService}
import uk.ac.warwick.tabula.data.{Daoisms, AutowiringSmallGroupDaoComponent, SessionComponent, SmallGroupDaoComponent}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet

class GroupsetMembershipFixtureCommand extends CommandInternal[SmallGroupSet] with Logging{
	this: UserLookupComponent with SmallGroupDaoComponent with SessionComponent =>

	var groupSetId: String = _
	var userId: String = _

	protected def applyInternal() =
		transactional() {
			val user = userLookup.getUserByUserId(userId)
			val groupset = smallGroupDao.getSmallGroupSetById(groupSetId).get
			if (groupset.members.isEmpty){
				groupset._membersGroup = UserGroup.ofUsercodes // have to use usercodes in fixtures because test users cant be looked
			}		                                         // up by university ID
			groupset.members.add(user)
			logger.info(s"Added user $userId to groupset $groupSetId")
			groupset
		}
}

object GroupsetMembershipFixtureCommand{
	def apply() = {
		new GroupsetMembershipFixtureCommand
			with ComposableCommand[SmallGroupSet]
			with AutowiringUserLookupComponent
			with AutowiringSmallGroupDaoComponent
			with Daoisms
			with Unaudited
			with PubliclyVisiblePermissions
	}
}
