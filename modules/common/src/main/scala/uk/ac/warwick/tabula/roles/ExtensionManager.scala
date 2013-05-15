package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permissions._

case class ExtensionManager(department: Department) extends BuiltInRole(ExtensionManagerRoleDefinition, department)

case object ExtensionManagerRoleDefinition extends BuiltInRoleDefinition {
	
	override def description = "Extension Manager"
	
	GrantsScopedPermission( 
		Extension.ReviewRequest,
		Extension.Update,
		Extension.Read
	)

}