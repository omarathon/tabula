package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.commands.{Unaudited, ReadOnly, Command}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.ViewModules
import uk.ac.warwick.tabula.groups.web.controllers.GroupsDisplayHelper
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.services.SmallGroupServiceComponent
import uk.ac.warwick.tabula.services.AutowiringSmallGroupServiceComponent
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.system.permissions.RequiresPermissionsChecking
import uk.ac.warwick.tabula.system.permissions.PermissionsChecking

object ListStudentsGroupsCommand {
	def apply(member: Member) =
		new ListStudentsGroupsCommandInternal(member)
			with ComposableCommand[ViewModules]
			with ListStudentsGroupsCommandPermissions
			with AutowiringSmallGroupServiceComponent
			with Unaudited with ReadOnly
}

/**
 * Gets the data for a students view of all small groups they're a member of.
 */

class ListStudentsGroupsCommandInternal(val member: Member) extends CommandInternal[ViewModules] with ListStudentsGroupsCommandState {
	self: SmallGroupServiceComponent =>

	import GroupsDisplayHelper._

	def applyInternal() = {
		val user = member.asSsoUser
		val memberGroupSets = smallGroupService.findSmallGroupSetsByMember(user)
		val releasedMemberGroupSets = getGroupSetsReleasedToStudents(memberGroupSets)
		val nonEmptyMemberViewModules = getViewModulesForStudent(releasedMemberGroupSets, getGroupsToDisplay(_, user))

		ViewModules(nonEmptyMemberViewModules.sortBy(_.module.code), canManageDepartment = false)
	}

}

trait ListStudentsGroupsCommandState {
	def member: Member
}

trait ListStudentsGroupsCommandPermissions extends RequiresPermissionsChecking {
	self: ListStudentsGroupsCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.Read.SmallGroups, member)
	}
}