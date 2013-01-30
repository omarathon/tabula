package uk.ac.warwick.tabula.services.permissions

import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.roles.DepartmentalAdministrator
import uk.ac.warwick.tabula.roles.Role
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.roles.ModuleManager
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.roles.Marker
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.ExtensionManager

@Component
class ExtensionManagerRoleProvider extends RoleProvider {
	
	var moduleAndDepartmentService = Wire.auto[ModuleAndDepartmentService]

	def getRolesFor(user: CurrentUser, scope: => PermissionsTarget): Seq[Role] = {		
		scope match {
			case department: Department => 
				if (department.isExtensionManager(user.idForPermissions)) Seq(ExtensionManager(department))
				else Seq()
		}
	}
	
	def rolesProvided = Set(classOf[ExtensionManager])
	
}