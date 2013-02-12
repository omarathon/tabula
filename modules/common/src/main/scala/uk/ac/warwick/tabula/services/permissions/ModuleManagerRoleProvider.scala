package uk.ac.warwick.tabula.services.permissions
import scala.collection.JavaConversions._

import org.springframework.stereotype.Component

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.roles.ModuleManager
import uk.ac.warwick.tabula.roles.Role

@Component
class ModuleManagerRoleProvider extends RoleProvider {
	
	def getRolesFor(user: CurrentUser, scope: => PermissionsTarget): Seq[Role] =
		scope match {
			case department: Department => 
				department.modules filter {_.ensuredParticipants.includes(user.idForPermissions)} map {ModuleManager(_)}
			
			case module: Module =>
				if (module.ensuredParticipants.includes(user.idForPermissions)) Seq(ModuleManager(module))
				else Seq()
				
			// The only times we'd check for module manager permission is on a department or module
			case _ => Seq()
		}
	
	def rolesProvided = Set(classOf[ModuleManager])
	
}