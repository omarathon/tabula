package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent, UserLookupService}
import uk.ac.warwick.tabula.data.{Daoisms, AutowiringSmallGroupDaoComponent, SessionComponent, SmallGroupDaoComponent}
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.helpers.Logging

class GroupsetMembershipFixtureCommand extends CommandInternal[Unit] with Logging{
	this: UserLookupComponent with SmallGroupDaoComponent with SessionComponent =>

	var groupSetId: String = _
	var userId: String = _

	protected def applyInternal() {
		transactional() {
			val user = userLookup.getUserByUserId(userId)
			val groupset = smallGroupDao.getSmallGroupSetById(groupSetId).get
			groupset.members.add(user)
			logger.info(s"Added user $userId to groupset $groupSetId")
		}
	}
}

object GroupsetMembershipFixtureCommand{
	def apply() = {
		new GroupsetMembershipFixtureCommand
			with ComposableCommand[Unit]
			with AutowiringUserLookupComponent
			with AutowiringSmallGroupDaoComponent
			with Daoisms
			with Unaudited
			with PubliclyVisiblePermissions
	}
}
