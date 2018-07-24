package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.{CommandInternal, Describable, Description}
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails
import uk.ac.warwick.tabula.data.{StudentCourseDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.permissions.{PermissionsService, PermissionsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object RemoveStudentCourseYearDetailsCommand {

}

class RemoveStudentCourseYearDetailsCommandInternal
	extends CommandInternal[Seq[String]]
		with Logging {
	self: PermissionsServiceComponent
		with StudentCourseYearDetailsDaoComponent
	with StudentCourseDetailsDaoComponent =>
	override protected def applyInternal(): Seq[String] = {
		studentCourseYearDetailsDao.getIdsStaleSince(???)


		// normally, when scd is deleted, associated scyd would also be deleted
		// so this is really just checking for orphaned scyd
		val yearDetails: Seq[StudentCourseYearDetails] = studentCourseYearDetailsDao
			.getMissingFromImportBefore(DateTime.now.minusYears(1))

//		val courseDetails = studentCourseDetailsDao.getByScjCode(yearDetails.map(_.))

		// and check the scj code from course details,
		// if not-exist in course, then we remove this year details
		// of if the course details ended more than 6 years ago, we also delete the year details
		???
	}


}


trait RemoveStudentCourseYearDetailsCommandPermission
	extends RequiresPermissionsChecking
		with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}

trait RemoveStudentCourseYearDetailsCommandDescription extends Describable[Seq[String]] {
	override def describe(d: Description) {}

	override def describeResult(d: Description, result: Seq[String]): Unit = {
		d.studentIds(result)
	}
}