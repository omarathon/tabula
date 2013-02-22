package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsTarget

case class DepartmentModuleManager(department: model.Department) extends BuiltInRole(department, ModuleManagerRoleDefinition)
case class ModuleManager(module: model.Module) extends BuiltInRole(module, ModuleManagerRoleDefinition)

case object ModuleManagerRoleDefinition extends BuiltInRoleDefinition {
	
	GeneratesSubRole(ModuleAssistantRoleDefinition)
	
	GrantsScopedPermission( 	
		Assignment.Archive,
		Assignment.Delete,
		
		RolesAndPermissions.Create,
		RolesAndPermissions.Update,
		RolesAndPermissions.Delete,
		
		Submission.Delete,
		
		Feedback.Publish
	)

}