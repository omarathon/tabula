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
		Masquerade,
		GodMode,
		ManageMaintenanceMode,
		ImportSystemData,
		ReplicaSyncing,
		
		StudentRelationshipType.Create,
		StudentRelationshipType.Read,
		StudentRelationshipType.Update,
		StudentRelationshipType.Delete
	)
	
	GrantsGlobalPermission(
		Module.Create,
		// We don't give Read here, god up for that
		Module.Update,
		Module.Delete,
		
		Department.ArrangeModules,
		
		// To add department owners, but also global-read is for the permissions helper
		RolesAndPermissions.Create,
		RolesAndPermissions.Read,
		RolesAndPermissions.Update,
		RolesAndPermissions.Delete
	)
	
}