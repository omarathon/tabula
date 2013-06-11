package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.data.model.groups.SmallGroupSet
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.groups.SmallGroup
import uk.ac.warwick.tabula.helpers.Promise
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.helpers.StringUtils._
import org.joda.time.LocalTime

class CreateSmallGroupEventCommand(group: Promise[SmallGroup], module: Module) extends ModifySmallGroupEventCommand {
	import CreateSmallGroupEventCommand._
	
	PermissionCheck(Permissions.SmallGroups.Create, module)

	var service = Wire[SmallGroupService]

	def applyInternal() = transactional() {
		// We set the promised value here so that sub-commands work
		val event = { promisedValue = new SmallGroupEvent }
		copyTo(event)
		
		// FIXME This is to avoid the un-saved transient Hibernate bug
		if (group.get.id.hasText) service.saveOrUpdate(event)
		
		event
	}
	
	def isEmpty = weekRanges.isEmpty && day == null && startTime == DefaultStartTime && endTime == DefaultEndTime

	override def describeResult(d: Description, smallGroupEvent: SmallGroupEvent) = d.smallGroupEvent(smallGroupEvent)

	override def describe(d: Description) = d.smallGroup(group.get).properties()
	
}

object CreateSmallGroupEventCommand {
	val DefaultStartTime = new LocalTime(12, 0)
	val DefaultEndTime = DefaultStartTime.plusHours(1)
}