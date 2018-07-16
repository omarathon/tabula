package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.commands.{CommandInternal, Describable, Description}
import uk.ac.warwick.tabula.data.MemberDaoComponent
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.permissions.PermissionsServiceComponent

object RemovePersonalDataAfterRelationshipFinishedCommand {

}

class RemovePersonalDataAfterRelationshipFinishedCommandInternal extends CommandInternal[Seq[String]] with Logging {
	self: PermissionsServiceComponent with MemberDaoComponent =>
	override protected def applyInternal(): Seq[String] = {

		memberDao.getByUniversityId()

		???
	}
}

trait RemovePersonalDataAfterRelationshipFinishedCommandDescription extends Describable[Seq[String]] {
	override def describe(d: Description) {}
	override def describeResult(d: Description, result: Seq[String]): Unit = {
		d.users(???) //  get all the users removed
	}
}