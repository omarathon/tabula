package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.commands.{CommandInternal, Describable, Description}
import uk.ac.warwick.tabula.data.MemberDaoComponent
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.permissions.PermissionsServiceComponent

object RemovePersonalDataAfterCourseEndedCommand {

}

class RemovePersonalDataAfterCourseEndedCommandInternal extends CommandInternal[Seq[String]] with Logging {
	self: PermissionsServiceComponent with MemberDaoComponent =>
	override protected def applyInternal(): Seq[String] = {

		// get missing from import since date that's more than 6 yeats ago
		// and intersect with the one's course ended 6 years ago

		memberDao.getMissingSince()
		???
	}
}

trait RemovePersonalDataAfterCourseEndedCommandDescription extends Describable[Seq[String]] {
	override def describe(d: Description) {}
	override def describeResult(d: Description, result: Seq[String]): Unit = {
		d.users(???) //  get all the users removed
	}
}