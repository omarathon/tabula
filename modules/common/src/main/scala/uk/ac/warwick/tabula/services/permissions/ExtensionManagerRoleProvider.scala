package uk.ac.warwick.tabula.services.permissions
import scala.collection.JavaConversions._



import org.springframework.stereotype.Component

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.ExtensionManager
import uk.ac.warwick.tabula.roles.Role

@Component
class ExtensionManagerRoleProvider extends RoleProvider {
	
	def getRolesFor(user: CurrentUser, scope: => PermissionsTarget): Seq[Role] = {		
		scope match {
			case department: Department => 
				if (department.isExtensionManager(user.idForPermissions)) Seq(ExtensionManager(department))
				else Seq()
			
			// Extension managers are only checked at department level
			case _ => Seq()
		}
	}
	
	def rolesProvided = Set(classOf[ExtensionManager])
	
}