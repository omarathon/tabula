package uk.ac.warwick.tabula.groups.commands

import scala.collection.JavaConverters.asScalaBufferConverter

import org.springframework.validation.Errors

import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.commands.Describable
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.MemberOrUser
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.services.AutowiringSmallGroupServiceComponent
import uk.ac.warwick.tabula.services.AutowiringUserLookupComponent
import uk.ac.warwick.tabula.services.ProfileServiceComponent
import uk.ac.warwick.tabula.services.SmallGroupServiceComponent
import uk.ac.warwick.tabula.services.UserLookupComponent
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking

object RecordAttendanceCommand {
	def apply(event: SmallGroupEvent, week: Int) =
		new RecordAttendanceCommand(event, week)
			with ComposableCommand[SmallGroupEventOccurrence]
			with RecordAttendanceCommandPermissions
			with RecordAttendanceDescription
			with AutowiringSmallGroupServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringProfileServiceComponent
}

abstract class RecordAttendanceCommand(val event: SmallGroupEvent, val week: Int) 
	extends CommandInternal[SmallGroupEventOccurrence] 
			with Appliable[SmallGroupEventOccurrence] with RecordAttendanceState with SelfValidating {
	self: SmallGroupServiceComponent with UserLookupComponent with ProfileServiceComponent =>
	type UniversityId = String

	var attendees: JList[UniversityId] = JArrayList()
	
	def populate() {
		attendees = smallGroupService.getAttendees(event, week)
		members = event.group.students.users map { user =>
			val member = profileService.getMemberByUniversityId(user.getWarwickId)
			MemberOrUser(member, user)
		}
	}

	def applyInternal(): SmallGroupEventOccurrence = {
		smallGroupService.updateAttendance(event, week, attendees.asScala)
	}

	def validate(errors: Errors) {
		val invalidUsers: Seq[UniversityId] = attendees.asScala.filter(s => !userLookup.getUserByWarwickUniId(s).isFoundUser())
		if (invalidUsers.length > 0) {
			errors.rejectValue("attendees", "smallGroup.attendees.invalid", Array(invalidUsers), "")
		} else {
			val missingUsers: Seq[UniversityId] = attendees.asScala.filter(s => event.group.students.users.filter(u => u.getWarwickId() == s).length == 0)
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
	var members: Seq[MemberOrUser] = _
}

trait RecordAttendanceDescription extends Describable[SmallGroupEventOccurrence] {
	this: RecordAttendanceState =>
	def describe(d: Description) {
		d.smallGroupEvent(event)
		d.property("week", week)
	}
}
