package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, ReadOnly, Unaudited}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

case class TimetableClashInformation(
	smallGroupSet: SmallGroupSet,
	timtableClashMembersPerGroup: Seq[(SmallGroup, Seq[User])]
)

object SmallGroupSetTimetableClashCommand {
	def apply(smallGroupSet: SmallGroupSet, user: CurrentUser) = {
		new SmallGroupSetTimetableClashCommandInternal(smallGroupSet, user)
			with ComposableCommand[TimetableClashInformation]
			with SmallGroupSetTimetableClashCommandPermissions
			with AutowiringSmallGroupServiceComponent
			with Unaudited with ReadOnly
	}
}

class SmallGroupSetTimetableClashCommandInternal(val smallGroupSet: SmallGroupSet, val user: CurrentUser)
	extends CommandInternal[TimetableClashInformation] with SmallGroupSetTimetableClashCommandState {
	self: SmallGroupServiceComponent =>

	override def applyInternal() = {
		TimetableClashInformation(smallGroupSet, smallGroupService.findPossibleTimetableClashesForGroupSet(smallGroupSet))
	}

}

trait SmallGroupSetTimetableClashCommandState {
	val user: CurrentUser
	val smallGroupSet: SmallGroupSet
}

trait SmallGroupSetTimetableClashCommandPermissions extends RequiresPermissionsChecking {
	self: SmallGroupSetTimetableClashCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.ReadMembership, smallGroupSet)
	}
}