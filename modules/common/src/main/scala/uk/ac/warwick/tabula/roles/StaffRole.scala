package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

case class StaffRole(department: model.Department) extends BuiltInRole(StaffRoleDefinition, department)

case object StaffRoleDefinition extends UnassignableBuiltInRoleDefinition {

	override def description = "Staff Member"

	GrantsScopelessPermission(
		UserPicker,
		MonitoringPointSetTemplates.View
	)

	GrantsGlobalPermission(
		Profiles.Search,
		Profiles.Read.Core // As per discussion in TAB-753, anyone at the University can see anyone else's core information
	)

	GrantsScopedPermission(
		Profiles.Read.StudentCourseDetails.Core,
		Profiles.Read.StudentCourseDetails.Status,
		Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType])
	)
}
