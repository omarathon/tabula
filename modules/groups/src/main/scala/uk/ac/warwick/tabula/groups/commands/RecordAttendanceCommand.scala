package uk.ac.warwick.tabula.groups.commands

import scala.collection.JavaConverters._

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import org.springframework.validation.Errors

object RecordAttendanceCommand {
	def apply(event: SmallGroupEvent, week: Int) =
		new RecordAttendanceCommand(event, week)
			with ComposableCommand[Unit]
			with RecordAttendanceCommandPermissions
			with RecordAttendanceDescription
			with AutowiringSmallGroupServiceComponent
			with AutowiringUserLookupComponent
}

abstract class RecordAttendanceCommand(val event: SmallGroupEvent, val week: Int) extends CommandInternal[Unit] with Appliable[Unit] with RecordAttendanceState with SelfValidating {
	self: SmallGroupServiceComponent with UserLookupComponent =>
	type UserId = String

	var attendees: JList[UserId] = JArrayList()

	def applyInternal() {
		//val users = userLookup.getUsersByUserIds(attendees)
		smallGroupService.updateAttendance(event, week, attendees.asScala)
	}

	def validate(errors: Errors) {
		???
	}
}


trait RecordAttendanceCommandPermissions extends RequiresPermissionsChecking {
	self: RecordAttendanceState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroupEvents.Register, event)
	}
}

trait RecordAttendanceState {
	val event: SmallGroupEvent
	val week: Int
}

trait RecordAttendanceDescription extends Describable[Unit] {
	this: RecordAttendanceState =>
	def describe(d: Description) {
		d.smallGroupEvent(event)
		d.property("week", week)
	}
}
