package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.{CommandInternal, Describable, Description}
import uk.ac.warwick.tabula.data.{StudentCourseDetailsDaoComponent, MemberDaoComponent}
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.permissions.PermissionsServiceComponent

object RemovePersonalDataAfterCourseEndedCommand {

}

class RemovePersonalDataAfterCourseEndedCommandInternal extends CommandInternal[Seq[String]] with Logging {
	self: PermissionsServiceComponent with MemberDaoComponent with StudentCourseDetailsDaoComponent =>
	override protected def applyInternal(): Seq[String] = {

		val sixYearsAgo = DateTime.now().minusYears(6)
		memberDao.getMissingBefore[Member](sixYearsAgo) // gone from SITS
			.map(studentCourseDetailsDao.getByStudentUniversityId)
  			.
//			.map(_.student.universityId)

		???
	}
}

trait RemovePersonalDataAfterCourseEndedCommandDescription extends Describable[Seq[String]] {
	override def describe(d: Description) {}

	override def describeResult(d: Description, result: Seq[String]): Unit = {
		d.users(???) //  get all the users removed
	}
}