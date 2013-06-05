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

class EditSmallGroupSetCommand(set: SmallGroupSet) extends ModifySmallGroupSetCommand(set.module) {
	
	PermissionCheck(Permissions.SmallGroups.Update, set)
	
	this.copyFrom(set)
	promisedValue = set
	
	var service = Wire[SmallGroupService]

	def applyInternal() = transactional() {
		copyTo(set)
		service.saveOrUpdate(set)
		set
	}

	override def describe(d: Description) = d.smallGroupSet(set).properties(
		"name" -> name,
		"format" -> format)
	
}