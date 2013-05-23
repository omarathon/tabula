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

class CreateSmallGroupSetCommand(module: Module) extends ModifySmallGroupSetCommand(module) {
	
	PermissionCheck(Permissions.SmallGroups.Create, module)
	
	var service = Wire[SmallGroupService]

	def applyInternal() = transactional() {
		// We set the promised value here so that sub-commands work
		val set = setPromisedValue(new SmallGroupSet(module))
		copyTo(set)
		service.saveOrUpdate(set)
		set
	}

	override def describeResult(d: Description, smallGroupSet: SmallGroupSet) = d.smallGroupSet(smallGroupSet)

	override def describe(d: Description) = d.module(module).properties(
		"name" -> name,
		"format" -> format)
	
}