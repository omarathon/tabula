package uk.ac.warwick.tabula.scheduling.commands.imports

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{CommandInternal, ComposableCommand, Description, Unaudited}
import uk.ac.warwick.tabula.data.{AutowiringMemberDaoComponent, AutowiringStudentCourseYearDetailsDaoComponent, MemberDao, MemberDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.scheduling.services.{AutowiringTier4ImporterComponent, Tier4RequirementImporter, Tier4RequirementImporterComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ImportTier4ForStudentCommand {
	def apply(student: StudentMember, year: AcademicYear) =
		new ImportTier4ForStudentCommandInternal(student, year)
			with ComposableCommand[Boolean]
			with ImportTier4ForStudentCommandPermissions
			with AutowiringTier4ImporterComponent
			with AutowiringMemberDaoComponent
			with Unaudited
}

class ImportTier4ForStudentCommandInternal(student: StudentMember, year: AcademicYear) extends CommandInternal[Boolean] {

	self: Tier4RequirementImporterComponent with MemberDaoComponent =>

	def applyInternal(): Boolean = {
		val newRequirement = tier4RequirementImporter.hasTier4Requirement(student.universityId)
		if (student.tier4VisaRequirement != newRequirement) {
			student.tier4VisaRequirement = newRequirement
			memberDao.saveOrUpdate(student)
			true
		}
		else false
	}
}

trait ImportTier4ForStudentCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ImportSystemData)
	}
}
