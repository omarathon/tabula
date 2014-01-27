package uk.ac.warwick.tabula.scheduling.commands.imports

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Unaudited}
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseYearDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.scheduling.services.{AutowiringCasUsageImporterComponent, CasUsageImporterComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ImportCasUsageForStudentCommand {
	def apply(student: StudentMember, year: AcademicYear) =
		new ImportCasUsageForStudentCommandInternal(student, year)
			with ComposableCommand[Unit]
			with ImportCasUsageForStudentCommandPermissions
			with AutowiringCasUsageImporterComponent
			with AutowiringStudentCourseYearDetailsDaoComponent
			with Unaudited
}

class ImportCasUsageForStudentCommandInternal(student: StudentMember, year: AcademicYear) extends CommandInternal[Unit] {

	self: CasUsageImporterComponent with StudentCourseYearDetailsDaoComponent =>

	def applyInternal() = {
		var changed = false
		val newCasUsed = casUsageImporter.isCasUsed(student.universityId)
		student.freshOrStaleStudentCourseYearDetails(year).map {
			scyd => {
				if (scyd.casUsed != newCasUsed) {
					scyd.casUsed = newCasUsed
					studentCourseYearDetailsDao.saveOrUpdate(scyd)
					changed = true
				}
			}
		}
		changed
	}
}

trait ImportCasUsageForStudentCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}
