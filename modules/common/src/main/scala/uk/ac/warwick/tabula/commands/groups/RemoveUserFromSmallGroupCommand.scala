package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.commands.Description

class RemoveUserFromSmallGroupCommand(val user: User, val group: SmallGroup) extends Command[Unit] {
	
	PermissionCheck(Permissions.SmallGroups.Update, group)
	
	def applyInternal() {
		group.students.remove(user)
	}

	override def describe(d: Description) = 
		d.smallGroup(group).properties(
			"usercode" -> user.getUserId,
			"universityId" -> user.getWarwickId
		)

}