package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking, PermissionsCheckingMethods}
import uk.ac.warwick.tabula.permissions.Permissions.Profiles

object ViewModuleRegistrationsCommand {
	def apply(studentCourseYearDetails: StudentCourseYearDetails) =
		new ViewModuleRegistrationsCommandInternal(studentCourseYearDetails) with
			ComposableCommand[Seq[ModuleRegistration]] with
			ViewModuleRegistrationsCommandPermissions with
			ReadOnly with Unaudited
}

trait ViewModuleRegistrationsCommandState {
	val studentCourseYearDetails: StudentCourseYearDetails
}

class ViewModuleRegistrationsCommandInternal(val studentCourseYearDetails: StudentCourseYearDetails)
	extends CommandInternal[Seq[ModuleRegistration]] with ViewModuleRegistrationsCommandState {

	def applyInternal() = {
			studentCourseYearDetails.moduleRegistrations
	}
}

trait ViewModuleRegistrationsCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ViewModuleRegistrationsCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Profiles.Read.ModuleRegistration.Core, studentCourseYearDetails.studentCourseDetails.student)
	}
}