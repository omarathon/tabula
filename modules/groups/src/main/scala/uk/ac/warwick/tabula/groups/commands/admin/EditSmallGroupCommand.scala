package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.commands.{BaseDescribable, Describable, Description}
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SmallGroupService

class EditSmallGroupCommand(val group: SmallGroup, properties: SmallGroupSetProperties)
	extends ModifySmallGroupCommand(group.groupSet.module, properties)
	with EditSmallGroupCommandDescription
	with EditSmallGroupCommandState {

	PermissionCheck(Permissions.SmallGroups.Update, group)
	
	this.copyFrom(group)
	promisedValue = group
	
	var service = Wire[SmallGroupService]

	def applyInternal() = transactional() {
		copyTo(group)
		group
	}
}

trait EditSmallGroupCommandState {
	def name: String
	val group: SmallGroup
}

trait EditSmallGroupCommandDescription extends BaseDescribable[SmallGroup] {
	self: EditSmallGroupCommandState =>
	override def describe(d: Description) {
		d.smallGroup(group)
		d.property("name" -> name)
	}
}