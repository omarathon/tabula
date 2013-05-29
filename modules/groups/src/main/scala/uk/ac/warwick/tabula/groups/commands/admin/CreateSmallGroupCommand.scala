package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.helpers.Promise
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.helpers.StringUtils._

class CreateSmallGroupCommand(groupSet: Promise[SmallGroupSet], module: Module) extends ModifySmallGroupCommand(module) {
	
	PermissionCheck(Permissions.SmallGroups.Create, module)

	var service = Wire[SmallGroupService]

	def applyInternal() = transactional() {
		// We set the promised value here so that sub-commands work
		val group = setPromisedValue(new SmallGroup(groupSet.get))
		copyTo(group)
		
		// FIXME This is to avoid the un-saved transient Hibernate bug
		if (groupSet.get.id.hasText) service.saveOrUpdate(group)
		
		group
	}

	override def describeResult(d: Description, smallGroup: SmallGroup) = d.smallGroup(smallGroup)

	override def describe(d: Description) = d.smallGroupSet(groupSet.get).properties(
		"name" -> name)
	
}