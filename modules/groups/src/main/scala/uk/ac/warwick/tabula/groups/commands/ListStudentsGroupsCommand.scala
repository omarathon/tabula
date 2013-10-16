package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.data.model.{Member, Module}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.commands.{Unaudited, ReadOnly, Command}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod


trait ListStudentsGroupsCommand extends Applicable[Map[Module, Map[SmallGroupSet, Seq[SmallGroup]]]]

/**
 * Gets the data for a students view of all small groups they're a member of.
 */

class ListStudentsGroupsCommandImpl(member: Member)
	extends Command[Map[Module, Map[SmallGroupSet, Seq[SmallGroup]]]]
	with TutorHomeCommand
	with ReadOnly
	with Unaudited {

	PermissionCheck(Permissions.Profiles.Read.SmallGroups, member)

	var smallGroupService = Wire[SmallGroupService]

	def applyInternal() =
		smallGroupService.findSmallGroupsByStudent(member.asSsoUser)
			.filter(_.groupSet.visibleToStudents)
			.groupBy { group => group.groupSet }
			.groupBy { case (set, groups) => set.module }

}
