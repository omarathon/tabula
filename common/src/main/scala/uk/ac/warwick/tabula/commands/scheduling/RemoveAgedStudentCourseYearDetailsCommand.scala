package uk.ac.warwick.tabula.commands.scheduling

import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Describable, Description}
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseYearDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.permissions.{AutowiringPermissionsServiceComponent, PermissionsServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object RemoveAgedStudentCourseYearDetailsCommand {
	def apply() =
		new RemoveAgedStudentCourseYearDetailsCommandInternal
			with ComposableCommand[Seq[String]]
			with AutowiringPermissionsServiceComponent
			with AutowiringStudentCourseYearDetailsDaoComponent
			with RemoveAgedStudentCourseYearDetailsCommandPermission
			with RemoveAgedStudentCourseYearDetailsCommandDescription
}

class RemoveAgedStudentCourseYearDetailsCommandInternal
	extends CommandInternal[Seq[String]] with Logging {
	self: PermissionsServiceComponent
		with StudentCourseYearDetailsDaoComponent =>
	override protected def applyInternal(): Seq[String] = (for {
		ids <- Some(studentCourseYearDetailsDao.getOrphaned)
	} yield {
		studentCourseYearDetailsDao.deleteByIds(ids)
		ids
	}).getOrElse(Seq.empty)
}


trait RemoveAgedStudentCourseYearDetailsCommandPermission
	extends RequiresPermissionsChecking
		with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}

trait RemoveAgedStudentCourseYearDetailsCommandDescription extends Describable[Seq[String]] {
	override def describe(d: Description) {}

	override def describeResult(d: Description, result: Seq[String]): Unit = {
		d.properties(("ScydIds", result))
	}
}