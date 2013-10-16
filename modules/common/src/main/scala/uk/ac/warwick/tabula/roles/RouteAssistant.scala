package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.JavaImports

case class RouteAssistant(route: model.Route) extends BuiltInRole(RouteAssistantRoleDefinition, route)

case object RouteAssistantRoleDefinition extends BuiltInRoleDefinition {
	
	override def description = "RouteAssistant"

	GrantsScopedPermission( 
		MonitoringPoints.View,
		MonitoringPoints.Record
	)

	def canDelegateThisRolesPermissions: JavaImports.JBoolean = true
}