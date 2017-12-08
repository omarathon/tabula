package uk.ac.warwick.tabula.commands.admin.department

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, ManualMembershipInfo}


object ManualMembershipSummaryCommand {
	def apply(department: Department) = new ManualMembershipSummaryCommandInternal(department)
		with ComposableCommand[ManualMembershipInfo]
		with ReadOnly with Unaudited
		with ManualMembershipSummaryPermissions
		with AutowiringAssessmentMembershipServiceComponent
}

class ManualMembershipSummaryCommandInternal(val department: Department) extends CommandInternal[ManualMembershipInfo]
	with ManualMembershipSummaryState with CurrentAcademicYear {

	self: AssessmentMembershipServiceComponent =>

	def applyInternal(): ManualMembershipInfo = {
		assessmentMembershipService.departmentsManualMembership(department, academicYear)
	}
}

trait ManualMembershipSummaryPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: ManualMembershipSummaryState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ViewManualMembershipSummary, department)
	}
}

trait ManualMembershipSummaryState {
	val department: Department
}