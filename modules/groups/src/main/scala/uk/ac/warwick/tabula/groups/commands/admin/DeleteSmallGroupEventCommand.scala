package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object DeleteSmallGroupEventCommand {
	def apply(group: SmallGroup, event: SmallGroupEvent) =
		new DeleteSmallGroupEventCommandInternal(group, event)
			with ComposableCommand[SmallGroupEvent]
			with DeleteSmallGroupEventPermissions
			with DeleteSmallGroupEventDescription
}

trait DeleteSmallGroupEventCommandState {
	def group: SmallGroup
	def event: SmallGroupEvent
}

class DeleteSmallGroupEventCommandInternal(val group: SmallGroup, val event: SmallGroupEvent) extends CommandInternal[SmallGroupEvent] with DeleteSmallGroupEventCommandState {

	override def applyInternal() = {
		group.removeEvent(event)
		event
	}

}

trait DeleteSmallGroupEventPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DeleteSmallGroupEventCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(event, group)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(event))
	}
}

trait DeleteSmallGroupEventDescription extends Describable[SmallGroupEvent] {
	self: DeleteSmallGroupEventCommandState =>

	override def describe(d: Description) {
		d.smallGroupEvent(event)
	}
}