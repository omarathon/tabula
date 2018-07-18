package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description}
import uk.ac.warwick.tabula.data.{AutowiringMemberDaoComponent, AutowiringStudentCourseDetailsDaoComponent, MemberDaoComponent, StudentCourseDetailsDaoComponent}
import uk.ac.warwick.tabula.data.model.{Member, StudentMember}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object RemovePersonalDataAfterCourseEndedCommand {
	def apply() =
		new RemovePersonalDataAfterCourseEndedCommandInternal
			with ComposableCommand[Seq[String]]
			with AutowiringMemberDaoComponent
			with AutowiringStudentCourseDetailsDaoComponent
			with AutowiringPermissionsServiceComponent
			with RemovePersonalDataAfterCourseEndedCommandPermission
			with RemovePersonalDataAfterCourseEndedCommandDescription
}

class RemovePersonalDataAfterCourseEndedCommandInternal extends CommandInternal[Seq[String]] with Logging {
	self: PermissionsServiceComponent with MemberDaoComponent with StudentCourseDetailsDaoComponent =>
	override protected def applyInternal(): Seq[String] = {
		val sixYearsAgo = DateTime.now().minusYears(6)
		memberDao.deleteByUniversityIds(memberDao.getMissingBefore[Member](sixYearsAgo) // member missing from SITS
			.map(studentCourseDetailsDao.getByUniversityId)
			.map(_.filter(_.endDate != null))
			.map(_.filter(_.missingFromImportSince != null))
			.map(_.sortWith((l, r) => l.endDate.isBefore(r.endDate))) // course details that ends latest
			.flatMap(_.tail)
			.filter { details =>
				details.endDate.isBefore(sixYearsAgo.toLocalDate) && details.missingFromImportSince.isBefore(sixYearsAgo)
				// ended and missing 6+ years ago
			}
			.map(_.student.universityId))
	}
}


trait RemovePersonalDataAfterCourseEndedCommandPermission extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}

trait RemovePersonalDataAfterCourseEndedCommandDescription extends Describable[Seq[String]] {
	override def describe(d: Description) {}

	override def describeResult(d: Description, result: Seq[String]): Unit = {
		d.studentIds(result)
	}
}