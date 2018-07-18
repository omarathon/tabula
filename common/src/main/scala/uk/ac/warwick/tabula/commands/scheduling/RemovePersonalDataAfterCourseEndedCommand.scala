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
		memberDao.deleteByUniversityIds(memberDao.getMissingBefore[Member](sixYearsAgo) // member missing from SITS
			.map(studentCourseDetailsDao.getByStudentUniversityId)
			.map(_.filter(details => details.endDate != null && details.missingFromImportSince != null)) // has endDate and is missing
			.map(_.sortWith((l, r) => l.endDate.isBefore(r.endDate))).tail // course details that ends latest
			.flatten
			.filter { details =>
				details.endDate.isBefore(sixYearsAgo.toLocalDate) && details.missingFromImportSince.isBefore(sixYearsAgo)
				// ended and missing 6+ years ago
			}
			.map(_.student.universityId))
	}
}

trait RemovePersonalDataAfterCourseEndedCommandDescription extends Describable[Seq[String]] {
	override def describe(d: Description) {}

	override def describeResult(d: Description, result: Seq[String]): Unit = {
		d.studentIds(result)
	}
}