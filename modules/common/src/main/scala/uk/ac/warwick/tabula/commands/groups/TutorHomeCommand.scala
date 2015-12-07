package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroup, SmallGroupSet}
import uk.ac.warwick.tabula.services.{SmallGroupService}
import uk.ac.warwick.tabula.system.permissions.Public

trait TutorHomeCommand extends Appliable[Map[Module, Map[SmallGroupSet, Seq[SmallGroup]]]]

/** Gets the data for a tutor's view of all small groups they're tutor of.
  *
  * Permission is Public because it doesn't rely on permission of any one thing -
  * by definition it only returns data that is associated with you.
  */
class TutorHomeCommandImpl(user: CurrentUser)
	extends Command[Map[Module, Map[SmallGroupSet, Seq[SmallGroup]]]]
	with TutorHomeCommand
	with ReadOnly
	with Unaudited
	with Public {

	var smallGroupService = Wire[SmallGroupService]

	def applyInternal() =
		smallGroupService.findReleasedSmallGroupsByTutor(user)
			.groupBy { group => group.groupSet }
			.groupBy { case (set, groups) => set.module }

}
