package uk.ac.warwick.tabula.groups.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupSet, SmallGroup}
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.system.permissions.Public

// TODO extend shared trait when CM and RA have committed it.
trait Applicable[A] {
	def apply(): A
}

trait TutorHomeCommand extends Applicable[Map[Module, Seq[SmallGroupSet]]]
/** Gets the data for a tutor's view of a department's small groups.
  *
  * Permission is Public because it doesn't rely on permission of any one thing -
  * just at least one small group event in the department. And that's all the command
  * returns so we're only showing them what they have access to anyway, which might be
  * nothing.
  */
class TutorHomeCommandImpl(user: CurrentUser)
	extends Command[Map[Module, Seq[SmallGroupSet]]]
	with TutorHomeCommand
	with ReadOnly
	with Unaudited
	with Public {

	var smallGroupService = Wire[SmallGroupService]

	// We get all smallgroups then filter to the correct department here; in most cases
	// you wouldn't be tutor of a huge number of departments so this shouldn't be too wasteful.
	def applyInternal() =
		smallGroupService.findSmallGroupsByTutor(user.apparentUser)
			.map { _.groupSet }
			.distinct
			.groupBy { _.module }

}
