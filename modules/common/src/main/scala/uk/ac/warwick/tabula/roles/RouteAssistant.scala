package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.JavaImports
import uk.ac.warwick.tabula.permissions.PermissionsSelector
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

case class RouteAssistant(route: model.Route) extends BuiltInRole(RouteAssistantRoleDefinition, route)

case object RouteAssistantRoleDefinition extends BuiltInRoleDefinition {
	
	override def description = "Route Assistant"
	
	GeneratesSubRole(RouteAuditorRoleDefinition)

	GrantsScopedPermission(
		MonitoringPoints.Record,
		
		Profiles.StudentRelationship.Create(PermissionsSelector.Any[StudentRelationshipType]),
		Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]),
		Profiles.StudentRelationship.Update(PermissionsSelector.Any[StudentRelationshipType]),
		Profiles.StudentRelationship.Delete(PermissionsSelector.Any[StudentRelationshipType]),

		MemberNotes.Create,
		MemberNotes.Update,
		MemberNotes.Delete
	)

	def canDelegateThisRolesPermissions: JavaImports.JBoolean = true
}