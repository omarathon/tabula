package uk.ac.warwick.tabula.groups.commands

import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence

object RecordAttendanceCommand {
	def apply(event: SmallGroupEvent, week: Int) =
		new RecordAttendanceCommand(event, week)
			with ComposableCommand[SmallGroupEventOccurrence]
			with RecordAttendanceCommandPermissions
			with RecordAttendanceDescription
			with AutowiringSmallGroupServiceComponent
			with AutowiringUserLookupComponent
}

abstract class RecordAttendanceCommand(val event: SmallGroupEvent, val week: Int) 
	extends CommandInternal[SmallGroupEventOccurrence] 
			with Appliable[SmallGroupEventOccurrence] with RecordAttendanceState with SelfValidating {
	self: SmallGroupServiceComponent with UserLookupComponent =>
	type UserId = String

	var attendees: JList[UserId] = JArrayList()
	
	def populate() {
		attendees = smallGroupService.getAttendees(event, week)
	}

	def applyInternal(): SmallGroupEventOccurrence = {
		smallGroupService.updateAttendance(event, week, attendees.asScala)
	}

	def validate(errors: Errors) {
		val invalidUsers: Seq[String] = attendees.asScala.filter(s => !userLookup.getUserByUserId(s).isFoundUser())
		if (invalidUsers.length > 0) {
			errors.rejectValue("attendees", "smallGroup.attendees.invalid", Array(invalidUsers), "")
		} else {
			val missingUsers: Seq[String] = attendees.asScala.filter(s => event.group.students.users.filter(u => u.getUserId() == s).length == 0)
			if (missingUsers.length > 0) {
				errors.rejectValue("attendees", "smallGroup.attendees.missing", Array(missingUsers), "")
			}
		}
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

trait RecordAttendanceDescription extends Describable[SmallGroupEventOccurrence] {
	this: RecordAttendanceState =>
	def describe(d: Description) {
		d.smallGroupEvent(event)
		d.property("week", week)
	}
}
