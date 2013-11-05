package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.commands.{Unaudited, ReadOnly, Command}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.ViewModules
import uk.ac.warwick.tabula.groups.web.controllers.GroupsDisplayHelper
import uk.ac.warwick.tabula.CurrentUser

trait ListStudentsGroupsCommand extends Applicable[ViewModules]

/**
 * Gets the data for a students view of all small groups they're a member of.
 */

class ListStudentsGroupsCommandImpl(member: Member, currentUser: CurrentUser)
	extends Command[ViewModules]
	with ReadOnly
	with Unaudited {

	import GroupsDisplayHelper._

	PermissionCheck(Permissions.Profiles.Read.SmallGroups, member)

	var smallGroupService = Wire[SmallGroupService]

	def applyInternal() = {

		val user = member.asSsoUser
		val memberGroupSets = smallGroupService.findSmallGroupSetsByMember(user)
		val releasedMemberGroupSets = getGroupSetsReleasedToStudents(memberGroupSets)
		val isTutor = !(currentUser.apparentUser == user)
		val nonEmptyMemberViewModules = getViewModulesForStudent(releasedMemberGroupSets, getGroupsToDisplay(_, user, isTutor))

		ViewModules(nonEmptyMemberViewModules.sortBy(_.module.code), canManageDepartment = false)

	}

}
