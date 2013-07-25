package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Unaudited, ReadOnly, Command}
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.SmallGroupService


trait ListStudentsGroupsCommand extends Applicable[Map[Module, Map[SmallGroupSet, Seq[SmallGroup]]]]

/** Gets the data for a students view of all small groups they're a member of.
  *
  * Permission is Public because it doesn't rely on permission of any one thing -
  * by definition it only returns data that is associated with you.
  */
class ListStudentsGroupsCommandImpl(user: CurrentUser)
	extends Command[Map[Module, Map[SmallGroupSet, Seq[SmallGroup]]]]
	with TutorHomeCommand
	with ReadOnly
	with Unaudited
	with Public {

	var smallGroupService = Wire[SmallGroupService]

	def applyInternal() =
		smallGroupService.findSmallGroupsByStudent(user.apparentUser)
			.groupBy { group => group.groupSet }
			.groupBy { case (set, groups) => set.module }

}
