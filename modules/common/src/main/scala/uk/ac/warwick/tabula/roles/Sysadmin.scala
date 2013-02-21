package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._

case class Sysadmin extends BuiltInRole(None, SysadminRoleDefinition)

case object SysadminRoleDefinition extends BuiltInRoleDefinition {
	
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
		PermissionsHelper,
		ManageAllDepartmentPermissions
	)
	
	GrantsGlobalPermission(
		Module.Create,
		// We don't give Read here, god up for that
		Module.Update,
		Module.Delete,
		
		// To add department owners
		Department.ManagePermissions
	)
	
}