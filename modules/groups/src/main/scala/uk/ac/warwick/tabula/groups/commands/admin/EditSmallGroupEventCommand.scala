package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.helpers.Promise
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SmallGroupService

class EditSmallGroupEventCommand(event: SmallGroupEvent) extends ModifySmallGroupEventCommand {
	
	PermissionCheck(Permissions.SmallGroups.Update, event)
	
	this.copyFrom(event)
	promisedValue = event
	
	var service = Wire[SmallGroupService]

	def applyInternal() = transactional() {
		copyTo(event)
		service.saveOrUpdate(event)
		event
	}

	override def describe(d: Description) = d.smallGroupEvent(event).properties()
	
}