package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.helpers.Promise
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SmallGroupService

class EditSmallGroupCommand(group: SmallGroup) extends ModifySmallGroupCommand(group.groupSet.module) {

	PermissionCheck(Permissions.SmallGroups.Update, group)
	
	this.copyFrom(group)
	promisedValue = group
	
	var service = Wire[SmallGroupService]

	def applyInternal() = transactional() {
		copyTo(group)
		group
	}

	override def describe(d: Description) = d.smallGroup(group).properties(
		"name" -> name)
	
}