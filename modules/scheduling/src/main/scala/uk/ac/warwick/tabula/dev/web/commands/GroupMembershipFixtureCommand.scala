package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.data.{Daoisms, AutowiringSmallGroupDaoComponent, SessionComponent, SmallGroupDaoComponent}
import uk.ac.warwick.tabula.data.Transactions._
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class GroupMembershipFixtureCommand extends CommandInternal[Unit] with Logging{
	this: UserLookupComponent with SmallGroupDaoComponent with SessionComponent =>

	var groupSetId: String = _
	var groupName: String = _
	var userId: String = _

	protected def applyInternal() {
		transactional() {
			val user = userLookup.getUserByUserId(userId)
			val groupset = smallGroupDao.getSmallGroupSetById(groupSetId).get
			val group = groupset.groups.asScala.find(_.name == groupName).get
			group.students.add(user)
			logger.info(s"Added user $userId to group $groupName  in groupset $groupSetId")
		}
	}
}
object GroupMembershipFixtureCommand {
	def apply() = {
		new GroupMembershipFixtureCommand
			with ComposableCommand[Unit]
			with AutowiringUserLookupComponent
			with AutowiringSmallGroupDaoComponent
			with Daoisms
			with Unaudited
			with PubliclyVisiblePermissions
	}
}
