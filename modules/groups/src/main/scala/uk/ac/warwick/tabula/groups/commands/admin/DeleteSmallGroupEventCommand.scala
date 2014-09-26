package uk.ac.warwick.tabula.groups.commands.admin

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupEvent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import scala.collection.JavaConverters._

object DeleteSmallGroupEventCommand {
	def apply(group: SmallGroup, event: SmallGroupEvent) =
		new DeleteSmallGroupEventCommandInternal(group, event)
			with ComposableCommand[SmallGroupEvent]
			with DeleteSmallGroupEventPermissions
			with DeleteSmallGroupEventDescription
			with DeleteSmallGroupEventValidation
			with AutowiringSmallGroupServiceComponent
}

trait DeleteSmallGroupEventCommandState {
	def group: SmallGroup
	def event: SmallGroupEvent
}

class DeleteSmallGroupEventCommandInternal(val group: SmallGroup, val event: SmallGroupEvent) extends CommandInternal[SmallGroupEvent] with DeleteSmallGroupEventCommandState {
	self: SmallGroupServiceComponent =>

	override def applyInternal() = transactional() {
		smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event).foreach(smallGroupService.delete)
		group.events.remove(event)
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

trait DeleteSmallGroupEventValidation extends SelfValidating {
	self: DeleteSmallGroupEventCommandState with SmallGroupServiceComponent =>

	def validate(errors: Errors) {
		// Can't delete events that have attendance recorded against them
		val hasAttendance =
			smallGroupService.getAllSmallGroupEventOccurrencesForEvent(event)
				.exists { _.attendance.asScala.exists { attendance =>
					attendance.state != AttendanceState.NotRecorded
				}}

		if (hasAttendance) {
			errors.rejectValue("event", "smallGroupEvent.delete.hasAttendance")
		}
	}
}