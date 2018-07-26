package uk.ac.warwick.tabula.commands.scheduling

import org.joda.time.DateTime
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description}
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseDetailsDaoComponent, StudentCourseDetailsDaoComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsService, PermissionsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object RemoveAgedStudentCourseDetailsCommand {
	def apply() =
		new RemoveAgedStudentCourseDetailsCommandInternal
			with ComposableCommand[Seq[String]]
			with AutowiringPermissionsServiceComponent
			with AutowiringStudentCourseDetailsDaoComponent
			with RemoveAgedStudentCourseDetailsCommandPermission
			with RemoveAgedStudentCourseDetailsCommandDescription
}

class RemoveAgedStudentCourseDetailsCommandInternal
	extends CommandInternal[Seq[String]]
		with Logging {
	self: PermissionsServiceComponent
		with StudentCourseDetailsDaoComponent =>

	override protected def applyInternal(): Seq[String] = (for {
		ids <- Some(studentCourseDetailsDao
			.getByEndDateBefore(DateTime.now.minusYears(6))
			.filter(_.missingFromImportSince != null)
			.filter(_.missingFromImportSince.isBefore(DateTime.now().minusYears(1)))
			.map(_.scjCode))
	} yield {
		studentCourseDetailsDao.deleteByIds(ids)
		ids
	}).getOrElse(Seq.empty)
}

trait RemoveAgedStudentCourseDetailsCommandPermission
	extends RequiresPermissionsChecking
		with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}

trait RemoveAgedStudentCourseDetailsCommandDescription extends Describable[Seq[String]] {
	override def describe(d: Description) {}

	override def describeResult(d: Description, result: Seq[String]): Unit = {
		d.properties(("ScdIds", result))
	}
}