package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permissions._

case class ExtensionManager(department: Department) extends BuiltInRole(department) {
	
	GrantsPermissionFor(department, 
		Extension.ReviewRequest,
		Extension.Update,
		Extension.Read
	)

}