package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._

case class Sysadmin() extends BuiltInRole(SysadminRoleDefinition, None)

case object SysadminRoleDefinition extends UnassignableBuiltInRoleDefinition {
	
	override def description = "Tabula Administrator"
	
	/*
	 * IMPORTANT
	 * 
	 * A Sysadmin does *NOT* gain any additional permissions past the sysadmin-actions by default; that's what god mode is for  
	 */
	
	GrantsScopelessPermission(
		GodMode,
		ManageMaintenanceMode,
		ImportSystemData,
		ReplicaSyncing,
		ViewAuditLog,
		
		StudentRelationshipType.Create,
		StudentRelationshipType.Read,
		StudentRelationshipType.Update,
		StudentRelationshipType.Delete,

		MonitoringPointTemplates.Manage
	)

	GrantsGlobalPermission(
		Masquerade,
		Department.Create,
		Module.Create,
		// We don't give Read here, god up for that
		Module.Update,
		Module.Delete,
		
		Department.ArrangeModules,
		Department.ArrangeRoutes
	)
	
}