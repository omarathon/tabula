package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._

case class AcademicOfficeUser() extends BuiltInRole(AcademicOfficeRoleDefinition, None)

case object AcademicOfficeRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Academic office administrator"

	GrantsGlobalPermission(
		Department.ViewManualMembershipSummary
	)

}
