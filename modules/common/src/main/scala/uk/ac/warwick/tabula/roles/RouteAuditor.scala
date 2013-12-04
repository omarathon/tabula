package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.JavaImports

case class RouteAuditor(route: model.Route) extends BuiltInRole(RouteAuditorRoleDefinition, route)

case object RouteAuditorRoleDefinition extends BuiltInRoleDefinition {
	
	override def description = "Route Auditor"

	GrantsScopedPermission( 
		MonitoringPoints.View
	)

	def canDelegateThisRolesPermissions: JavaImports.JBoolean = false
}